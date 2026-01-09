package com.example.common.util;

import com.example.common.dto.UserInfo;

import java.util.UUID;

/**
 * Utility class for security-related operations
 */
public final class SecurityUtils {
    
    private static final ThreadLocal<UserInfo> CURRENT_USER = new ThreadLocal<>();
    
    private SecurityUtils() {
        // Utility class
    }
    
    /**
     * Set the current user context (typically called by JWT filter)
     */
    public static void setCurrentUser(UserInfo userInfo) {
        CURRENT_USER.set(userInfo);
    }
    
    /**
     * Get the current user context
     */
    public static UserInfo getCurrentUser() {
        return CURRENT_USER.get();
    }
    
    /**
     * Get current user ID or null if not authenticated
     */
    public static UUID getCurrentUserId() {
        UserInfo user = CURRENT_USER.get();
        return user != null ? user.getUserId() : null;
    }
    
    /**
     * Get current username or null if not authenticated
     */
    public static String getCurrentUsername() {
        UserInfo user = CURRENT_USER.get();
        return user != null ? user.getUsername() : null;
    }
    
    /**
     * Clear the current user context (typically called after request completes)
     */
    public static void clear() {
        CURRENT_USER.remove();
    }
    
    /**
     * Check if current user has a specific role
     */
    public static boolean hasRole(String role) {
        UserInfo user = CURRENT_USER.get();
        return user != null && user.getRoles() != null && user.getRoles().contains(role);
    }
    
    /**
     * Check if current user has a specific permission
     */
    public static boolean hasPermission(String permission) {
        UserInfo user = CURRENT_USER.get();
        return user != null && user.getPermissions() != null && user.getPermissions().contains(permission);
    }
}
