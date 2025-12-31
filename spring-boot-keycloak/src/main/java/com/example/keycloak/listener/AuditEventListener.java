package com.example.keycloak.listener;

import com.example.keycloak.entity.AuditLog;
import com.example.keycloak.event.*;
import com.example.keycloak.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener xử lý audit logging cho tất cả domain events
 * Sử dụng @Async để không block main thread
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {
    
    private final AuditLogRepository auditLogRepository;
    
    @Async
    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        log.info("AUDIT: Authentication success for user: {}", event.getUsername());
        saveAuditLog(
            "LOGIN_SUCCESS",
            "USER",
            event.getUsername(),
            event.getUsername(),
            String.format("User %s logged in successfully via %s", 
                event.getUsername(), event.getAuthType())
        );
    }
    
    @Async
    @EventListener
    public void handleAuthenticationFailed(AuthenticationFailedEvent event) {
        log.warn("AUDIT: Authentication failed for user: {} - Reason: {}", 
            event.getUsername(), event.getFailureReason());
        saveAuditLog(
            "LOGIN_FAILED",
            "USER",
            event.getUsername(),
            event.getUsername(),
            String.format("Login failed via %s: %s", 
                event.getAuthType(), event.getFailureReason())
        );
    }
    
    @Async
    @EventListener
    public void handleTicketCreated(TicketCreatedEvent event) {
        log.info("AUDIT: Ticket created - ID: {}, Maker: {}", 
            event.getTicketId(), event.getMaker());
        saveAuditLog(
            "TICKET_CREATED",
            "TICKET",
            event.getTicketId().toString(),
            event.getMaker(),
            String.format("Created ticket '%s' with amount %s", 
                event.getTitle(), event.getAmount())
        );
    }
    
    @Async
    @EventListener
    public void handleTicketStatusChanged(TicketStatusChangedEvent event) {
        String action = "TICKET_" + event.getNewStatus().name();
        String details = String.format("Status changed from %s to %s", 
            event.getPreviousStatus(), event.getNewStatus());
        
        if (event.getReason() != null) {
            details += " - Reason: " + event.getReason();
        }
        
        log.info("AUDIT: Ticket {} status changed to {} by {}", 
            event.getTicketId(), event.getNewStatus(), event.getChangedBy());
        saveAuditLog(
            action,
            "TICKET",
            event.getTicketId().toString(),
            event.getChangedBy(),
            details
        );
    }
    
    @Async
    @EventListener
    public void handlePasswordChanged(PasswordChangedEvent event) {
        log.info("AUDIT: Password {} for user: {}", event.getChangeType(), event.getUsername());
        saveAuditLog(
            "PASSWORD_" + event.getChangeType(),
            "USER",
            event.getUsername(),
            event.getUsername(),
            String.format("Password %s successfully", event.getChangeType().toLowerCase())
        );
    }
    
    private void saveAuditLog(String action, String entityType, String entityId, 
                              String userId, String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .userId(userId)
                .details(details)
                .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage());
        }
    }
}
