package com.example.auth.dto.password;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for password change operation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordResponse {
    
    private Boolean success;
    
    private String message;
}
