package com.example.keycloak.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RejectionRequest {
    @NotBlank(message = "Reason is required")
    private String reason;
}
