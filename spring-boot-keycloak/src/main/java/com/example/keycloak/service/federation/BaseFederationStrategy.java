package com.example.keycloak.service.federation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base Federation Strategy
 *
 * Features:
 */
public abstract class BaseFederationStrategy implements FederationStrategy {

    protected static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    // Rate limiting / Account lockout configuration
    protected static final int MAX_FAILED_ATTEMPTS = 5;
    protected static final long LOCKOUT_DURATION_MS = 15 * 60 * 1000; // 15 minutes

    // Shared state across all strategies (in production, use Redis)
    protected static final ConcurrentHashMap<String, AtomicInteger> failedAttempts = new ConcurrentHashMap<>();
    protected static final ConcurrentHashMap<String, Long> lockoutTime = new ConcurrentHashMap<>();

    /**
     * Check if account is locked due to too many failed attempts
     */
    protected boolean isAccountLocked(String username) {
        Long lockedUntil = lockoutTime.get(username);
        if (lockedUntil == null) {
            return false;
        }

        if (System.currentTimeMillis() > lockedUntil) {
            lockoutTime.remove(username);
            failedAttempts.remove(username);
            return false;
        }

        return true;
    }

    /**
     * Record a failed login attempt
     */
    protected void recordFailedAttempt(String username) {
        AtomicInteger attempts = failedAttempts.computeIfAbsent(username, k -> new AtomicInteger(0));
        int currentAttempts = attempts.incrementAndGet();

        if (currentAttempts >= MAX_FAILED_ATTEMPTS) {
            lockoutTime.put(username, System.currentTimeMillis() + LOCKOUT_DURATION_MS);
            auditLog.warn("ACCOUNT_LOCKED | user={} | attempts={} | duration_minutes={}",
                    username, currentAttempts, LOCKOUT_DURATION_MS / 60000);
        }
    }

    /**
     * Reset failed attempts after successful login
     */
    protected void resetFailedAttempts(String username) {
        failedAttempts.remove(username);
        lockoutTime.remove(username);
    }
}
