package com.example.auth.dto.password;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for reset code verification
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyResetCodeResponse {
    
    private String verificationToken;
    
    private Boolean verified;
    
    private String message;
}
