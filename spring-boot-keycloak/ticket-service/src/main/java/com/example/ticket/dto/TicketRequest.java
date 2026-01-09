package com.example.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketRequest {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    private BigDecimal amount;
}
