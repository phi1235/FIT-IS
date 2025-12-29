package com.example.keycloak.strategy;

import com.example.keycloak.dto.LoginRequest;
import com.example.keycloak.dto.LoginResponse;
import com.example.keycloak.provider.CustomUser;
import com.example.keycloak.provider.CustomUserRepository;
import com.example.keycloak.service.JwtService;
import com.example.keycloak.service.RemoteFederationService;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Strategy cho Federation Authentication
 * Keycloak chỉ validate credentials (200/401)
 * Backend sinh JWT token locally
 */
@Slf4j
@Component
public class FederationAuthenticationStrategy implements AuthenticationStrategy {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private RemoteFederationService remoteFederationService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JwtService jwtService;

    @Value("${keycloak.auth-server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Override
    public LoginResponse authenticate(LoginRequest request) throws AuthenticationException {
        try {
            log.info("Authenticating user {} with Federation Provider", request.getUsername());

            // Validate với Remote User Federation Provider
            boolean isValid = remoteFederationService.validateCredentials(
                    request.getUsername(),
                    request.getPassword());

            if (!isValid) {
                throw new AuthenticationException(
                        "Invalid credentials from federation",
                        "FEDERATION_AUTH_FAILED");
            }

            // Validate với Keycloak
            try {
                validateWithKeycloak(request.getUsername(), request.getPassword());
                log.info("Keycloak validated credentials for user: {}", request.getUsername());
            } catch (Exception e) {
                log.warn("Keycloak validation skipped for federation user: {}", request.getUsername());
                // Continue - federation validation đã pass
            }

            // Lấy user info từ database
            LoginResponse.UserInfo userInfo = getUserInfoFromDatabase(request.getUsername());

            // Sinh JWT token locally
            String accessToken = jwtService.generateToken(
                    userInfo.getUsername(),
                    userInfo.getRole(),
                    userInfo.getId(),
                    userInfo.getEmail());
            String refreshToken = jwtService.generateRefreshToken(userInfo.getUsername());

            LocalDateTime now = LocalDateTime.now();
            long expiresInSeconds = jwtService.getExpirationTimeInSeconds();
            long refreshExpiresInSeconds = jwtService.getRefreshExpirationTimeInSeconds();
            LocalDateTime expiresAt = now.plusSeconds(expiresInSeconds);

            log.info("Generated local JWT token for federation user: {}", request.getUsername());

            return LoginResponse.builder()
                    .success(true)
                    .message("Login successful")
                    .user(userInfo)
                    .token(LoginResponse.TokenInfo.builder()
                            .accessToken(accessToken)
                            .refreshToken(refreshToken)
                            .tokenType("Bearer")
                            .expiresIn(expiresInSeconds)
                            .refreshExpiresIn(refreshExpiresInSeconds)
                            .build())
                    .metadata(LoginResponse.Metadata.builder()
                            .authProvider("Remote User Federation (Local JWT)")
                            .issuedAt(now.format(FORMATTER))
                            .expiresAt(expiresAt.format(FORMATTER))
                            .build())
                    .build();

        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Federation authentication failed for user {}: {}", request.getUsername(), e.getMessage());
            throw new AuthenticationException(
                    "Federation authentication failed",
                    "FEDERATION_ERROR",
                    e);
        }
    }

    /**
     * Validate credentials với Keycloak (optional)
     */
    private void validateWithKeycloak(String username, String password) throws Exception {
        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakServerUrl)
                .realm(realm)
                .clientId(clientId)
                .username(username)
                .password(password)
                .build();

        // Gọi API để validate
        keycloak.tokenManager().getAccessToken();
        keycloak.close();
    }

    private LoginResponse.UserInfo getUserInfoFromDatabase(String username) {
        try (Connection connection = dataSource.getConnection()) {
            CustomUserRepository repository = new CustomUserRepository(connection);
            CustomUser user = repository.findByUsername(username);

            if (user != null) {
                return LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getRole() != null ? user.getRole() : "user")
                        .build();
            }
        } catch (Exception e) {
            log.warn("Could not fetch user info from database: {}", e.getMessage());
        }

        // Fallback if user not found in database
        return LoginResponse.UserInfo.builder()
                .username(username)
                .role("user")
                .build();
    }

    @Override
    public boolean supports(String authType) {
        return "federation".equalsIgnoreCase(authType) ||
                "ldap".equalsIgnoreCase(authType) ||
                "remote".equalsIgnoreCase(authType);
    }
}
