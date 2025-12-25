package com.example.keycloak.controller;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/**
 * Remote Authentication API Controller
 * 
 * These endpoints are called by RemoteUserStorageProvider (Keycloak User
 * Federation)
 * to authenticate users from PostgreSQL database via REST API.
 * 
 * Flow: Keycloak -> RemoteUserStorageProvider -> This API -> PostgreSQL
 */
@RestController
@RequestMapping("/api")
public class RemoteAuthController {

    private static final Logger log = LoggerFactory.getLogger(RemoteAuthController.class);
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    // Should match the secret in RemoteUserStorageProvider
    private static final String API_SECRET = "default-secret-change-in-production";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long REQUEST_VALIDITY_SECONDS = 300; // 5 minutes

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Get user by username, email, or id
     * Called by RemoteUserStorageProvider for user lookup
     */
    @GetMapping
    public ResponseEntity<?> getUser(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String id,
            @RequestHeader(value = "X-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Signature", required = false) String signature) {

        log.info("Remote API: getUser called with username={}, email={}, id={}", username, email, id);

        try {
            Map<String, Object> user = null;

            if (username != null) {
                user = findUserByField("username", username);
            } else if (email != null) {
                user = findUserByField("email", email);
            } else if (id != null) {
                user = findUserByField("id", id);
            }

            if (user != null) {
                // Don't return password hash to remote caller
                user.remove("password");
                log.info("Remote API: User found: {}", user.get("username"));
                return ResponseEntity.ok(user);
            }

            log.info("Remote API: User not found");
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Remote API: Error getting user: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Validate user credentials
     * Called by RemoteUserStorageProvider.isValid() for password verification
     */
    @PostMapping("/login")
    public ResponseEntity<?> validateCredentials(@RequestBody Map<String, String> request,
            @RequestHeader(value = "X-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Signature", required = false) String signature) {

        String username = request.get("username");
        // Plain password sent from RemoteUserStorageProvider (over HTTPS in production)
        String password = request.get("password");

        log.info("Remote API: Login attempt for user: {}", username);

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and password required"));
        }

        try {
            // Find user in database
            Map<String, Object> user = findUserByField("username", username);

            if (user == null) {
                auditLog.warn("REMOTE_API_AUTH_FAILED | user={} | reason=USER_NOT_FOUND", username);
                return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
            }

            // Check if account is enabled
            Boolean enabled = (Boolean) user.get("enabled");
            if (enabled == null || !enabled) {
                auditLog.warn("REMOTE_API_AUTH_FAILED | user={} | reason=ACCOUNT_DISABLED", username);
                return ResponseEntity.status(401).body(Map.of("error", "Account disabled"));
            }

            String storedPasswordHash = (String) user.get("password");

            // Verify password using BCrypt
            boolean isValid = verifyBcryptPassword(password, storedPasswordHash);

            if (isValid) {
                auditLog.info("REMOTE_API_AUTH_SUCCESS | user={}", username);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "username", username,
                        "message", "Authentication successful"));
            } else {
                auditLog.warn("REMOTE_API_AUTH_FAILED | user={} | reason=INVALID_PASSWORD", username);
                return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
            }

        } catch (Exception e) {
            log.error("Remote API: Login error for user {}: {}", username, e.getMessage());
            auditLog.error("REMOTE_API_AUTH_ERROR | user={} | error={}", username, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Verify password using BCrypt constant-time comparison
     */
    private boolean verifyBcryptPassword(String plainPassword, String storedBcryptHash) {
        if (plainPassword == null || storedBcryptHash == null) {
            return false;
        }

        try {
            String trimmedHash = storedBcryptHash.trim();
            return BCrypt.checkpw(plainPassword, trimmedHash);
        } catch (IllegalArgumentException e) {
            log.error("Invalid BCrypt hash format: {}", e.getMessage());
            // Fallback for legacy plaintext passwords (not recommended!)
            if (!storedBcryptHash.startsWith("$2a$") && !storedBcryptHash.startsWith("$2b$")) {
                log.warn("SECURITY WARNING: Plaintext password detected!");
                return storedBcryptHash.equals(plainPassword);
            }
            return false;
        }
    }

    private Map<String, Object> findUserByField(String field, String value) {
        String sql = "SELECT id, username, email, password, first_name, last_name, enabled, role " +
                "FROM users WHERE " + field + " = ?";

        try {
            return jdbcTemplate.queryForMap(sql, value);
        } catch (Exception e) {
            log.debug("User not found by {}: {}", field, value);
            return null;
        }
    }

    /**
     * Validate HMAC signature for request authenticity
     */
    private boolean validateSignature(String method, String url, String body,
            String timestamp, String signature) {
        if (timestamp == null || signature == null) {
            log.warn("Missing timestamp or signature in request");
            return false;
        }

        try {
            // Check timestamp validity (prevent replay attacks)
            long requestTime = Long.parseLong(timestamp);
            long currentTime = Instant.now().getEpochSecond();
            if (Math.abs(currentTime - requestTime) > REQUEST_VALIDITY_SECONDS) {
                log.warn("Request timestamp expired");
                return false;
            }

            // Verify HMAC signature
            String dataToSign = method + "|" + url + "|" + body + "|" + timestamp;
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    API_SECRET.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hmacBytes);

            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            log.error("Signature validation error: {}", e.getMessage());
            return false;
        }
    }
}
