package com.example.auth.dto.password;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for password set operation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SetNewPasswordResponse {
    
    private Boolean success;
    
    private String message;
    
    private String redirectUrl;
}
