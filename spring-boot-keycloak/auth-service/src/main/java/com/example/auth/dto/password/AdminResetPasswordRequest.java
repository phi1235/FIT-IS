package com.example.auth.dto.password;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for admin password reset
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminResetPasswordRequest {
    
    @NotBlank(message = "User ID is required")
    private UUID userId;
    
    private String reason;
}
