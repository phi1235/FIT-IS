package com.example.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventDTO {
    private UUID id;
    private String eventType;
    private LocalDateTime eventTime;
    private UUID userId;
    private String username;
    private String sourceIp;
    private String userAgent;
    private String httpMethod;
    private String requestUrl;
    private Integer statusCode;
    private String errorMessage;
    private String metadata;
    private LocalDateTime createdAt;
}
