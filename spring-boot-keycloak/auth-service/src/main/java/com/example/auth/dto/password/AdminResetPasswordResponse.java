package com.example.auth.dto.password;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for admin password reset
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminResetPasswordResponse {
    
    private String sessionId;
    
    private String resetCode;
    
    private LocalDateTime expiresAt;
    
    private Integer codeExpiryMinutes;
    
    private String message;
}
