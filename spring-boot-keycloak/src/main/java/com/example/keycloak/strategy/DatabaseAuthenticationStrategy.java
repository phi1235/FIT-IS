package com.example.keycloak.strategy;

import com.example.keycloak.dto.LoginRequest;
import com.example.keycloak.dto.LoginResponse;
import com.example.keycloak.provider.CustomUser;
import com.example.keycloak.provider.CustomUserRepository;
import com.example.keycloak.service.JwtService;
import com.example.keycloak.service.RsaService;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Strategy cho Database Authentication với RSA password encryption
 * 
 * Flow:
 * 1. Frontend encrypts password with RSA public key
 * 2. Backend decrypts to get plain password
 * 3. Verify against stored BCrypt hash (with backward compatibility for v2
 * users)
 * 4. If v2 user verified, migrate to v1 format (simple BCrypt)
 * 5. Generate local JWT token
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
    private RsaService rsaService;

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
            // Decrypt RSA username if encrypted, otherwise use plain
            String plainUsername = decryptFieldIfNeeded(request.getUsername(), "Username");

            log.info("Authenticating user {} with Database Provider", plainUsername);

            // Get user info
            CustomUser customUser = getUserFromDatabase(plainUsername);

            if (customUser == null) {
                throw new AuthenticationException("User not found", "USER_NOT_FOUND");
            }

            // Decrypt RSA password if encrypted, otherwise use plain
            String plainPassword = decryptFieldIfNeeded(request.getPassword(), "Password");

            int passwordVersion = customUser.getPasswordVersion();
            log.info("User {} has password version: {}", plainUsername, passwordVersion);

            // Verify password with backward compatibility
            boolean isValid = verifyPassword(plainPassword, customUser.getPassword(), passwordVersion);

            if (!isValid) {
                throw new AuthenticationException("Invalid credentials", "INVALID_CREDENTIALS");
            }

            log.info("Password verified for user: {}", plainUsername);

            // Migrate v2 users to v1 (simple BCrypt) on successful login
            if (passwordVersion == 2) {
                try {
                    boolean migrated = migrateToSimpleBcrypt(plainUsername, plainPassword);
                    if (migrated) {
                        log.info("User {} migrated from v2 to v1 (simple BCrypt)", plainUsername);
                        passwordVersion = 1;
                    }
                } catch (Exception e) {
                    log.warn("Failed to migrate user {} to v1: {}", plainUsername, e.getMessage());
                }
            }

            // Generate JWT token
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

            log.info("Generated local JWT token for user: {}", plainUsername);

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
                            .authProvider("Database Provider (Local JWT + RSA)")
                            .issuedAt(now.format(FORMATTER))
                            .expiresAt(expiresAt.format(FORMATTER))
                            .passwordVersion(passwordVersion)
                            .build())
                    .build();

        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Database authentication failed: {}", e.getMessage());
            throw new AuthenticationException(
                    "Invalid credentials",
                    "INVALID_CREDENTIALS",
                    e);
        }
    }

    /**
     * Decrypt RSA-encrypted field or return as-is if plain
     */
    private String decryptFieldIfNeeded(String value, String fieldName) {
        if (rsaService.isEncrypted(value)) {
            log.debug("{} is RSA encrypted, decrypting...", fieldName);
            return rsaService.decrypt(value);
        }
        log.debug("{} is plain text (backward compatibility)", fieldName);
        return value;
    }

    /**
     * Verify password with backward compatibility for v2 users
     * v1: BCrypt(plain_password)
     * v2: BCrypt(SHA256(plain_password))
     */
    private boolean verifyPassword(String plainPassword, String storedHash, int passwordVersion) {
        try {
            if (passwordVersion == 2) {
                // v2: SHA256 + BCrypt
                String sha256Hash = hashSha256(plainPassword);
                return BCrypt.checkpw(sha256Hash, storedHash);
            } else {
                // v1: Simple BCrypt
                return BCrypt.checkpw(plainPassword, storedHash);
            }
        } catch (Exception e) {
            log.error("Password verification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Migrate v2 user to v1 format (simple BCrypt)
     */
    private boolean migrateToSimpleBcrypt(String username, String plainPassword) {
        try (Connection connection = dataSource.getConnection()) {
            String newHash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
            String sql = "UPDATE users SET password = ?, password_version = 1 WHERE username = ?";
            try (var stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, newHash);
                stmt.setString(2, username);
                int updated = stmt.executeUpdate();
                return updated > 0;
            }
        } catch (Exception e) {
            log.error("Failed to migrate password for user {}: {}", username, e.getMessage());
            return false;
        }
    }

    /**
     * Hash string with SHA-256
     */
    private String hashSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
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

    @Override
    public boolean supports(String authType) {
        return "database".equalsIgnoreCase(authType);
    }
}
