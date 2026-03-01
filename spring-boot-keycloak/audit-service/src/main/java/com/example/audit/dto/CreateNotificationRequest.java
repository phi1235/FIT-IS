package com.example.audit.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateNotificationRequest {
    private String title;
    private String content;
    private String type; // info | warning | success | danger
    private LocalDateTime expiresAt;
}
