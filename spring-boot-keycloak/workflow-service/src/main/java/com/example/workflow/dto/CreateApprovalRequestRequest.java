package com.example.workflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateApprovalRequestRequest {
    private String requestType;
    private String referenceId;
    private String businessKey;
    private String payload;
    private List<StepDefinition> steps;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepDefinition {
        private String stepName;
        private String approverType;
        private String approverRoleCode;
        private String approverUserId;
    }
}
