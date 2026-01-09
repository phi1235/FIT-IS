package com.example.auth.strategy;

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.LoginResponse;
import com.example.auth.entity.AuthUser;
import com.example.auth.entity.Role;
import com.example.auth.repository.AuthUserRepository;
import com.example.auth.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseAuthenticationStrategy implements AuthenticationStrategy {

    private final AuthUserRepository authUserRepository;
    private final JwtService jwtService;
    private final com.example.auth.service.RsaService rsaService;

    @Override
    public LoginResponse authenticate(LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        // Check for combined credentials format: KEY|PASSWORD|USERNAME
        if (request.getCredentials() != null && rsaService.isEncrypted(request.getCredentials())) {
            try {
                com.example.auth.service.RsaService.ParsedCredentials creds = rsaService.decryptCredentials(request.getCredentials());
                username = creds.getUsername();
                password = creds.getPassword();
                log.info("Decrypted credentials with combined format (KEY|PASSWORD|USERNAME)");
            } catch (Exception e) {
                log.error("Failed to decrypt combined credentials: {}", e.getMessage());
                throw new AuthenticationException("Invalid credential format", "AUTH_ERROR");
            }
        } else {
            // Fallback: Decrypt individual fields if they look like RSA-encrypted
            if (rsaService.isEncrypted(username)) {
                try {
                    username = rsaService.decrypt(username);
                    log.info("Decrypted username from RSA");
                } catch (Exception e) {
                    log.warn("Failed to decrypt username, using as-is: {}", e.getMessage());
                }
            }

            if (rsaService.isEncrypted(password)) {
                try {
                    password = rsaService.decrypt(password);
                    log.info("Decrypted password from RSA");
                } catch (Exception e) {
                    log.warn("Failed to decrypt password, using as-is: {}", e.getMessage());
                }
            }
        }

        log.info("Authenticating user {} with DATABASE strategy", username);

        try {
            AuthUser user = authUserRepository.findByUsername(username)
                    .orElseThrow(() -> new AuthenticationException("Invalid credentials", "INVALID_CREDENTIALS"));

            if (!user.isEnabled()) {
                log.warn("Account disabled: {}", username);
                throw new AuthenticationException("Account disabled", "ACCOUNT_DISABLED");
            }

            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
                log.warn("Account locked: {}", username);
                throw new AuthenticationException("Account locked", "ACCOUNT_LOCKED");
            }

            if (!BCrypt.checkpw(password, user.getPasswordHash())) {
                log.warn("Invalid password for user: {}", username);
                handleFailedLogin(user);
                throw new AuthenticationException("Invalid credentials", "INVALID_CREDENTIALS");
            }

            handleSuccessfulLogin(user);

            Set<String> roleCodes = new HashSet<>();
            if (user.getRoles() != null) {
                roleCodes = user.getRoles().stream()
                        .map(Role::getCode)
                        .collect(Collectors.toSet());
            }

            String accessToken = jwtService.generateToken(user.getUsername(), roleCodes, 
                    user.getId().toString(), user.getEmail());
            String refreshToken = jwtService.generateRefreshToken(user.getUsername());

            log.info("User authenticated successfully: {}", request.getUsername());

            return LoginResponse.builder()
                    .user(LoginResponse.UserInfo.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .roles(roleCodes)
                            .build())
                    .token(LoginResponse.TokenInfo.builder()
                            .accessToken(accessToken)
                            .refreshToken(refreshToken)
                            .tokenType("Bearer")
                            .expiresIn(jwtService.getExpirationTimeInSeconds())
                            .build())
                    .build();
                    
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Database authentication error: {}", e.getMessage(), e);
            throw new AuthenticationException("Authentication failed: " + e.getMessage(), "AUTH_ERROR");
        }
    }

    @Override
    public boolean supports(String authType) {
        return "database".equalsIgnoreCase(authType);
    }

    private void handleFailedLogin(AuthUser user) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        if (user.getFailedLoginAttempts() >= 5) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
        }
        authUserRepository.save(user);
    }

    private void handleSuccessfulLogin(AuthUser user) {
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        authUserRepository.save(user);
    }
}
