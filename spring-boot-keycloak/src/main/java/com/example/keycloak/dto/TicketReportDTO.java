package com.example.keycloak.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for JasperReports - status and createdAt are String to avoid format
 * issues
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketReportDTO {
    private Long id;
    private String title;
    private String status; // String instead of enum for JasperReports
    private BigDecimal amount;
    private String maker;
    private String checker;
    private String createdAt; // String formatted date (d-M-yyyy)
}
