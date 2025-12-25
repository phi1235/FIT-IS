package com.example.keycloak.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Data
public class TicketRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private BigDecimal amount;
}
