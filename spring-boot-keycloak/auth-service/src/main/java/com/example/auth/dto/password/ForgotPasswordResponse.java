package com.example.auth.dto.password;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for forgot password initiation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForgotPasswordResponse {
    
    private String sessionId;
    
    private String message;
    
    private LocalDateTime expiresAt;
    
    private Integer codeExpiryMinutes;
}
