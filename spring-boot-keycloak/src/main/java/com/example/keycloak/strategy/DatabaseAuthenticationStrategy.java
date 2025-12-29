package com.example.keycloak.strategy;

import com.example.keycloak.dto.LoginRequest;
import com.example.keycloak.dto.LoginResponse;
import com.example.keycloak.provider.CustomUser;
import com.example.keycloak.provider.CustomUserRepository;
import com.example.keycloak.service.JwtService;
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
 * Strategy cho Database Authentication
 * Keycloak chỉ validate credentials (200/401)
 * Backend sinh JWT token locally
 */
@Slf4j
@Component
public class DatabaseAuthenticationStrategy implements AuthenticationStrategy {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private com.example.keycloak.service.UserService userService;

    @Value("${keycloak.auth-server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Override
    public LoginResponse authenticate(LoginRequest request) throws AuthenticationException {
        try {
            log.info("Authenticating user {} with Database Provider", request.getUsername());

            // Get user info first to check password version
            CustomUser customUser = getUserFromDatabase(request.getUsername());

            if (customUser == null) {
                throw new AuthenticationException("User not found", "USER_NOT_FOUND");
            }

            int passwordVersion = customUser.getPasswordVersion();
            log.info("User {} has password version: {}", request.getUsername(), passwordVersion);

            // Validate with Keycloak
            validateWithKeycloak(request.getUsername(), request.getPassword());
            log.info("Keycloak validated credentials for user: {}", request.getUsername());

            // Tự động migrate password nếu đang ở version 1
            if (passwordVersion == 1) {
                log.info("User {} has password version 1, auto-migrating to version 2", request.getUsername());
                try {
                    boolean migrated = userService.autoMigratePassword(request.getUsername(), request.getPassword());
                    if (migrated) {
                        log.info("Password auto-migrated successfully for user: {}", request.getUsername());
                        // Reload user để lấy version mới
                        customUser = getUserFromDatabase(request.getUsername());
                        passwordVersion = customUser != null ? customUser.getPasswordVersion() : 2;
                    }
                } catch (Exception e) {
                    log.warn("Auto-migration failed for user {}: {}, continuing with version 1",
                            request.getUsername(), e.getMessage());
                    // Tiếp tục với version 1 nếu migration fail
                }
            }

            // Version 2 - Normal login flow
            LoginResponse.UserInfo userInfo = buildUserInfo(customUser);

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

            log.info("Generated local JWT token for user: {}", request.getUsername());

            return LoginResponse.builder()
                    .success(true)
                    .message("Login successful")
                    .requiresPasswordMigration(false)
                    .user(userInfo)
                    .token(LoginResponse.TokenInfo.builder()
                            .accessToken(accessToken)
                            .refreshToken(refreshToken)
                            .tokenType("Bearer")
                            .expiresIn(expiresInSeconds)
                            .refreshExpiresIn(refreshExpiresInSeconds)
                            .build())
                    .metadata(LoginResponse.Metadata.builder()
                            .authProvider("Database Provider (Local JWT)")
                            .issuedAt(now.format(FORMATTER))
                            .expiresAt(expiresAt.format(FORMATTER))
                            .passwordVersion(passwordVersion)
                            .build())
                    .build();

        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Database authentication failed for user {}: {}", request.getUsername(), e.getMessage());
            throw new AuthenticationException(
                    "Invalid credentials",
                    "INVALID_CREDENTIALS",
                    e);
        }
    }

    private CustomUser getUserFromDatabase(String username) {
        try (Connection connection = dataSource.getConnection()) {
            CustomUserRepository repository = new CustomUserRepository(connection);
            return repository.findByUsername(username);
        } catch (Exception e) {
            log.warn("Could not fetch user from database: {}", e.getMessage());
            return null;
        }
    }

    private LoginResponse.UserInfo buildUserInfo(CustomUser user) {
        return LoginResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole() != null ? user.getRole() : "user")
                .build();
    }

    /**
     * Validate credentials với Keycloak
     * Chỉ kiểm tra 200/401, không lấy token
     */
    private void validateWithKeycloak(String username, String password) throws AuthenticationException {
        try {
            Keycloak keycloak = KeycloakBuilder.builder()
                    .serverUrl(keycloakServerUrl)
                    .realm(realm)
                    .clientId(clientId)
                    .username(username)
                    .password(password)
                    .build();

            // Gọi API để validate - nếu thất bại sẽ throw exception
            keycloak.tokenManager().getAccessToken();

            keycloak.close();

        } catch (Exception e) {
            log.error("Keycloak validation failed for user {}: {}", username, e.getMessage());
            throw new AuthenticationException(
                    "Invalid credentials",
                    "KEYCLOAK_VALIDATION_FAILED",
                    e);
        }
    }

    @Override
    public boolean supports(String authType) {
        return "database".equalsIgnoreCase(authType);
    }
}
