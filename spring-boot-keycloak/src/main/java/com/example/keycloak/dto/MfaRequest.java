package com.example.keycloak.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * MFA Request DTO
 */
@Data
public class MfaRequest {
    
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "TOTP code is required")
    @Size(min = 6, max = 6, message = "TOTP code must be 6 digits")
    @Pattern(regexp = "^[0-9]{6}$", message = "TOTP code must be 6 digits")
    private String code;
}

