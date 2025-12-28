package com.example.keycloak.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Remote User Storage Provider cho Keycloak
 */
public class RemoteUserStorageProvider implements
        UserStorageProvider,
        UserLookupProvider,
        CredentialInputValidator {

    private static final Logger log = LoggerFactory.getLogger(RemoteUserStorageProvider.class);
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    // Security constants
    private static final int REQUEST_TIMEOUT_SECONDS = 10;
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final KeycloakSession session;
    private final ComponentModel model;
    private final String apiUrl;
    private final String apiSecret;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public RemoteUserStorageProvider(KeycloakSession session, ComponentModel model, String apiUrl) {
        this.session = session;
        this.model = model;
        // Normalize apiUrl: remove trailing slash if present
        String rawApiUrl = apiUrl;
        if (rawApiUrl != null && rawApiUrl.endsWith("/")) {
            rawApiUrl = rawApiUrl.substring(0, rawApiUrl.length() - 1);
        }
        this.apiUrl = rawApiUrl;
        // In production, this should come from secure configuration (Vault, HSM, etc.)
        this.apiSecret = model.get("apiSecret", "default-secret-change-in-production");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void close() {
    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        String externalId = StorageId.externalId(id);
        return fetchUserBy("id", externalId, realm);
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        return fetchUserBy("username", username, realm);
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        return fetchUserBy("email", email, realm);
    }

    private UserModel fetchUserBy(String field, String value, RealmModel realm) {
        try {
            String uriStr = String.format("%s?%s=%s", apiUrl, field, value);

            // Generate request signature for security
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String signature = generateHmacSignature("GET", uriStr, "", timestamp);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uriStr))
                    .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                    .header("Accept", "application/json")
                    .header("X-Timestamp", timestamp)
                    .header("X-Signature", signature)
                    .header("X-Request-ID", generateRequestId())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();
                if (body != null && !body.isEmpty()) {
                    CustomUser remoteUser = deserializeUser(body);
                    if (remoteUser != null) {
                        return new CustomUserAdapter(session, realm, model, remoteUser);
                    }
                }
            } else {
                log.warn("Fetch user failed for {}: {}. Status code: {}, Body: {}", field, value, response.statusCode(),
                        response.body());
            }
        } catch (Exception e) {
            log.error("Error fetching user by {}: {}", field, e.getMessage());
        }
        return null;
    }

    private CustomUser deserializeUser(String json) {
        try {
            if (json.startsWith("[")) {
                CustomUser[] users = objectMapper.readValue(json, CustomUser[].class);
                return users.length > 0 ? users[0] : null;
            } else {
                return objectMapper.readValue(json, CustomUser.class);
            }
        } catch (Exception e) {
            log.error("Error deserializing user: {}", e.getMessage());
            return null;
        }
    }

    // ========== CredentialInputValidator ==========

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return "password".equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return supportsCredentialType(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
        if (!supportsCredentialType(credentialInput.getType())) {
            return false;
        }

        String username = user.getUsername();
        String clientIp = getClientIpAddress();

        try {
            String password = credentialInput.getChallengeResponse();

            // The API endpoint will verify against BCrypt hash in database
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "username", username,
                    "password", password,
                    "timestamp", timestamp));

            String signature = generateHmacSignature("POST", apiUrl + "/login", requestBody, timestamp);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/login"))
                    .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("X-Timestamp", timestamp)
                    .header("X-Signature", signature)
                    .header("X-Request-ID", generateRequestId())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            boolean isValid = response.statusCode() == 200;

            if (isValid) {
                auditLog.info("REMOTE_AUTH_SUCCESS | user={} | ip={}", username, clientIp);
            } else {
                auditLog.warn("REMOTE_AUTH_FAILED | user={} | status={} | ip={}",
                        username, response.statusCode(), clientIp);
            }

            return isValid;

        } catch (Exception e) {
            log.error("Remote authentication error for user {}: {}", username, e.getMessage());
            auditLog.error("REMOTE_AUTH_ERROR | user={} | error={} | ip={}",
                    username, e.getMessage(), clientIp);
            return false;
        }
    }

    /**
     * Generate HMAC signature for API request
     * This ensures request integrity and authenticity
     */
    private String generateHmacSignature(String method, String url, String body, String timestamp) {
        try {
            String dataToSign = method + "|" + url + "|" + body + "|" + timestamp;

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    apiSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);

            byte[] hmacBytes = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);

        } catch (Exception e) {
            log.error("Error generating HMAC signature: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Hash password before transmission
     * Uses SHA-256 for one-way hashing
     */
    private String hashForTransmission(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Error hashing password for transmission: {}", e.getMessage());
            throw new RuntimeException("Password hashing failed", e);
        }
    }

    /**
     * Generate unique request ID for tracing
     */
    private String generateRequestId() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * Get client IP address for audit logging
     */
    private String getClientIpAddress() {
        try {
            if (session != null && session.getContext() != null &&
                    session.getContext().getConnection() != null) {
                return session.getContext().getConnection().getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not determine client IP: {}", e.getMessage());
        }
        return "unknown";
    }
}
