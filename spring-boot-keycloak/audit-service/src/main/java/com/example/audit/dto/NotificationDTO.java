package com.example.audit.dto;

import com.example.audit.entity.SystemNotification;
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
public class NotificationDTO {
    private UUID id;
    private String title;
    private String content;
    private String type;
    private String createdBy;
    private LocalDateTime createdAt;
    private boolean active;
    private LocalDateTime expiresAt;

    public static NotificationDTO from(SystemNotification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .title(n.getTitle())
                .content(n.getContent())
                .type(n.getType())
                .createdBy(n.getCreatedBy())
                .createdAt(n.getCreatedAt())
                .active(n.isActive())
                .expiresAt(n.getExpiresAt())
                .build();
    }
}
