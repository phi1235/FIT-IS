package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequestDTO {
    private UUID id;
    private String requestType;
    private String businessKey;
    private String referenceId;
    private String status;
    private UUID initiatorUserId;
    private Integer currentStep;
    private Integer totalSteps;
    private String payload;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private List<ApprovalStepDTO> steps;
    private List<ApprovalHistoryDTO> history;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalStepDTO {
        private UUID id;
        private Integer stepNumber;
        private String stepName;
        private String approverType;
        private String approverRoleCode;
        private UUID approverUserId;
        private String status;
        private String comments;
        private UUID actionBy;
        private LocalDateTime actionAt;
        private LocalDateTime dueDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalHistoryDTO {
        private UUID id;
        private UUID stepId;
        private String action;
        private UUID performedBy;
        private String comments;
        private String oldStatus;
        private String newStatus;
        private LocalDateTime performedAt;
    }
}
