package com.example.keycloak.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.AccessLevel;

/**
 * Login Request DTO với Bank-Level Validation
 * 
 * Security Features:
 * - Input length validation
 * - Character pattern validation (chống injection)
 * - No special characters in username
 */
@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._@-]+$", message = "Username can only contain letters, numbers, dots, underscores, @ and hyphens")
    @Setter(AccessLevel.NONE) // Không tạo setter tự động, dùng setter tùy chỉnh
    private String username;
    
    /**
     * Setter tự động trim khoảng trắng đầu cuối của username
     */
    @JsonProperty("username")
    public void setUsername(String username) {
        this.username = username != null ? username.trim() : null;
    }

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;
    
    /**
     * MFA/TOTP code (optional - required if user has MFA enabled)
     */
    @Size(min = 6, max = 6, message = "MFA code must be 6 digits")
    @Pattern(regexp = "^[0-9]{6}$", message = "MFA code must be 6 digits", flags = Pattern.Flag.CASE_INSENSITIVE)
    private String mfaCode;

    /**
     * Sanitize input to prevent log injection and other attacks
     */
    public String getSanitizedUsername() {
        if (username == null)
            return null;
        // Remove any control characters and limit length
        return username.replaceAll("[\\p{Cntrl}]", "").substring(0, Math.min(username.length(), 50));
    }
}
