package com.example.keycloak.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.AccessLevel;

/**
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
    @Setter(AccessLevel.NONE)
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
     * Sanitize input to prevent log injection and other attacks
     */
    public String getSanitizedUsername() {
        if (username == null)
            return null;
        return username.replaceAll("[\\p{Cntrl}]", "").substring(0, Math.min(username.length(), 50));
    }
}
