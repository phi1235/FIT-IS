package com.example.workflow.controller;

import com.example.common.util.SecurityUtils;
import com.example.workflow.dto.ApprovalRequestDTO;
import com.example.workflow.dto.CreateApprovalRequestRequest;
import com.example.workflow.dto.StepActionRequest;
import com.example.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @GetMapping("/requests")
    @PreAuthorize("hasAuthority('WORKFLOW_VIEW')")
    public ResponseEntity<Page<ApprovalRequestDTO>> getRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String requestType) {
        return ResponseEntity.ok(workflowService.getRequests(PageRequest.of(page, size), status, requestType));
    }

    @GetMapping("/requests/{id}")
    @PreAuthorize("hasAuthority('WORKFLOW_VIEW')")
    public ResponseEntity<ApprovalRequestDTO> getRequestById(@PathVariable UUID id) {
        return ResponseEntity.ok(workflowService.getRequestById(id));
    }

    @PostMapping("/requests")
    @PreAuthorize("hasAuthority('WORKFLOW_MANAGE')")
    public ResponseEntity<ApprovalRequestDTO> createRequest(@RequestBody CreateApprovalRequestRequest request) {
        UUID initiatorUserId = SecurityUtils.getCurrentUser() != null
                ? SecurityUtils.getCurrentUser().getUserId()
                : UUID.randomUUID();
        return ResponseEntity.ok(workflowService.createRequest(request, initiatorUserId));
    }

    @PostMapping("/requests/{requestId}/steps/{stepId}/action")
    @PreAuthorize("hasAuthority('WORKFLOW_MANAGE')")
    public ResponseEntity<ApprovalRequestDTO> processStep(
            @PathVariable UUID requestId,
            @PathVariable UUID stepId,
            @RequestBody StepActionRequest action) {
        UUID actorUserId = SecurityUtils.getCurrentUser() != null
                ? SecurityUtils.getCurrentUser().getUserId()
                : UUID.randomUUID();
        return ResponseEntity.ok(workflowService.processStep(requestId, stepId, action, actorUserId));
    }
}
