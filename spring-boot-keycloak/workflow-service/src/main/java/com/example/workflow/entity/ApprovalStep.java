package com.example.workflow.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "approval_step", schema = "workflow")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalStep {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "step_number", nullable = false)
    private Integer stepNumber;

    @Column(name = "step_name", nullable = false)
    private String stepName;

    @Column(name = "approver_type", nullable = false)
    private String approverType;

    @Column(name = "approver_role_code")
    private String approverRoleCode;

    @Column(name = "approver_user_id")
    private UUID approverUserId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "comments")
    private String comments;

    @Column(name = "action_by")
    private UUID actionBy;

    @Column(name = "action_at")
    private LocalDateTime actionAt;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
