package com.example.keycloak.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * DTO cho request đổi password
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {
    
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Current password is required")
    private String currentPassword;
    
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters")
    private String newPassword;
}
