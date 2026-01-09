package com.example.auth.model;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_audit_log", schema = "auth", indexes = {
    @Index(name = "idx_email_audit_user", columnList = "user_id"),
    @Index(name = "idx_email_audit_status", columnList = "status"),
    @Index(name = "idx_email_audit_template", columnList = "template_code"),
    @Index(name = "idx_email_audit_sent", columnList = "sent_at DESC")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmailAuditLog {
    
    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AuthUser user;
    
    @Column(nullable = false, length = 100)
    private String templateCode;
    
    @Column(nullable = false, length = 255)
    private String recipientEmail;
    
    @Column(length = 500)
    private String subject;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailStatus status;
    
    @Column(columnDefinition = "TEXT")
    private String statusReason;
    
    @Column(length = 45)
    private String ipAddress;
    
    @Column(columnDefinition = "TEXT")
    private String userAgent;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reset_token_id")
    private PasswordResetToken resetToken;
    
    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime sentAt;
    
    @Column
    private LocalDateTime updatedAt;
    
    public enum EmailStatus {
        SENT,
        FAILED,
        BOUNCE,
        COMPLAINT
    }
}
