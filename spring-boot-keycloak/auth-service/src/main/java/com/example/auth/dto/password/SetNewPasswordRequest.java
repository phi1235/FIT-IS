package com.example.auth.dto.password;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for setting new password after verification
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SetNewPasswordRequest {
    
    @NotBlank(message = "Verification token is required")
    private String verificationToken;
    
    @NotBlank(message = "New password is required")
    private String newPassword;
    
    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
}
