package com.example.keycloak.service.federation;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * External API Federation Strategy
 * Strategy Pattern Implementation cho External REST API Authentication
 */
@Component
public class ExternalApiFederationStrategy extends BaseFederationStrategy {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiFederationStrategy.class);

    @Override
    public boolean validate(String username, String password) {
        log.info("Validating user {} with External API", username);

        if (isAccountLocked(username)) {
            auditLog.warn("API_AUTH_BLOCKED | user={} | reason=ACCOUNT_LOCKED", username);
            return false;
        }

        try {
            // Mock: In production, call actual API with HMAC signature
            String storedHash = getStoredPasswordHash(username);
            if (storedHash == null) {
                auditLog.warn("API_AUTH_FAILED | user={} | reason=USER_NOT_FOUND", username);
                return false;
            }

            boolean isValid = BCrypt.checkpw(password, storedHash);

            if (isValid) {
                resetFailedAttempts(username);
                auditLog.info("API_AUTH_SUCCESS | user={}", username);
            } else {
                recordFailedAttempt(username);
                auditLog.warn("API_AUTH_FAILED | user={} | reason=INVALID_PASSWORD", username);
            }

            return isValid;

        } catch (Exception e) {
            log.error("API auth error for user {}: {}", username, e.getMessage());
            return false;
        }
    }

    private String getStoredPasswordHash(String username) {
        // Mock data - replace with actual API call
        if ("admin".equals(username)) {
            return BCrypt.hashpw("admin123", BCrypt.gensalt(12));
        }
        if ("user".equals(username)) {
            return BCrypt.hashpw("user123", BCrypt.gensalt(12));
        }
        return null;
    }

    @Override
    public String getType() {
        return "API";
    }

    @Override
    public boolean supports(String type) {
        return "api".equalsIgnoreCase(type) || "external".equalsIgnoreCase(type);
    }
}
