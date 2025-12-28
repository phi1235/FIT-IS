package com.example.keycloak.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * DTO for password migration request
 * Used when user with old password format needs to migrate to new SHA256 format
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MigratePasswordRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Current password is required")
    private String currentPassword; // SHA256 hashed from client

    @NotBlank(message = "New password is required")
    @Size(min = 64, max = 64, message = "Password must be SHA256 hashed (64 characters)")
    private String newPassword; // SHA256 hashed from client
}
