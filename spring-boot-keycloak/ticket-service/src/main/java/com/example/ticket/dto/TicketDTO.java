package com.example.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketDTO {
    private UUID id;
    private String code;
    private String title;
    private String description;
    private String status;
    private BigDecimal amount;
    private UUID makerUserId;
    private UUID checkerUserId;
    private String makerName;
    private String checkerName;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
