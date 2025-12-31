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
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Strategy cho Database Authentication với RSA password encryption
 * 
 * Flow:
 * 1. Frontend encrypts password with RSA public key
 * 2. Backend decrypts to get plain password
 * 3. Verify against stored BCrypt hash
 * 4. Generate local JWT token
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

            // Verify password with BCrypt
            boolean isValid = verifyPassword(plainPassword, customUser.getPassword());

            if (!isValid) {
                throw new AuthenticationException("Invalid credentials", "INVALID_CREDENTIALS");
            }

            log.info("Password verified for user: {}", plainUsername);

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
     * Verify password using BCrypt
     */
    private boolean verifyPassword(String plainPassword, String storedHash) {
        try {
            return BCrypt.checkpw(plainPassword, storedHash);
        } catch (Exception e) {
            log.error("Password verification failed: {}", e.getMessage());
            return false;
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
