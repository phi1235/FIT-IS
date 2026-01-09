package com.example.auth.model;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "password_reset_token", schema = "auth", indexes = {
    @Index(name = "idx_password_reset_token_user_id", columnList = "user_id"),
    @Index(name = "idx_password_reset_token_hash", columnList = "token_hash"),
    @Index(name = "idx_password_reset_expires", columnList = "expires_at"),
    @Index(name = "idx_password_reset_verified", columnList = "is_verified,is_consumed"),
    @Index(name = "idx_password_reset_created", columnList = "created_at DESC")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PasswordResetToken {
    
    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AuthUser user;
    
    @Column(nullable = false, unique = true, length = 255)
    private String tokenHash;
    
    @Column(nullable = false, length = 10)
    private String resetCode;
    
    @Column(nullable = false, length = 255)
    private String codeHash;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TokenType tokenType;
    
    @Column(nullable = false)
    private boolean verified = false;
    
    @Column(nullable = false)
    private boolean consumed = false;
    
    @Column
    private LocalDateTime verifiedAt;
    
    @Column
    private LocalDateTime consumedAt;
    
    @Column(length = 45)
    private String ipAddress;
    
    @Column(columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(nullable = false)
    private int failedAttempts = 0;
    
    @Column(nullable = false)
    private int maxAttempts = 5;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "created_by")
    private UUID createdBy;  // For admin resets
    
    public enum TokenType {
        FORGOT_PASSWORD,
        CHANGE_PASSWORD,
        ADMIN_RESET
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isValid() {
        return !consumed && !isExpired() && verified;
    }
    
    public void incrementFailedAttempts() {
        this.failedAttempts++;
    }
    
    public boolean isMaxAttemptsExceeded() {
        return failedAttempts >= maxAttempts;
    }
}
