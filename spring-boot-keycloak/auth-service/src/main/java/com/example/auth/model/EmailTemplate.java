package com.example.auth.model;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "email_template", schema = "auth", indexes = {
    @Index(name = "idx_email_template_code", columnList = "template_code"),
    @Index(name = "idx_email_template_active", columnList = "is_active"),
    @Index(name = "idx_email_template_created", columnList = "created_at DESC")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmailTemplate {
    
    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;
    
    @Column(unique = true, nullable = false, length = 100)
    private String templateCode;
    
    @Column(nullable = false, length = 255)
    private String templateName;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String subject;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String htmlBody;
    
    @Column(columnDefinition = "TEXT")
    private String textBody;
    
    @Column(columnDefinition = "TEXT")
    @Type(type = "com.vladmihalcea.hibernate.type.json.JsonType")
    private List<String> requiredVariables;
    
    @Column(nullable = false)
    private int version = 1;
    
    @Column(nullable = false)
    private boolean active = true;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private AuthUser createdBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", nullable = false)
    private AuthUser updatedBy;
    
    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
