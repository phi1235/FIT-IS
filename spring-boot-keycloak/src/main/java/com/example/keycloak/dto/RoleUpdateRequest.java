package com.example.keycloak.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * DTO để cập nhật role cho user
 */
@Data
public class RoleUpdateRequest {
    
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(admin|user)$", message = "Role must be 'admin' or 'user'")
    private String role;
}

