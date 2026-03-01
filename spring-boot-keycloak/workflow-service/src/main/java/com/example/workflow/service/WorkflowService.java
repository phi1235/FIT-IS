package com.example.workflow.service;

import com.example.workflow.dto.ApprovalRequestDTO;
import com.example.workflow.dto.CreateApprovalRequestRequest;
import com.example.workflow.dto.StepActionRequest;
import com.example.workflow.entity.ApprovalHistory;
import com.example.workflow.entity.ApprovalRequest;
import com.example.workflow.entity.ApprovalStep;
import com.example.workflow.repository.ApprovalHistoryRepository;
import com.example.workflow.repository.ApprovalRequestRepository;
import com.example.workflow.repository.ApprovalStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_SKIPPED = "SKIPPED";

    private final ApprovalRequestRepository requestRepository;
    private final ApprovalStepRepository stepRepository;
    private final ApprovalHistoryRepository historyRepository;

    public Page<ApprovalRequestDTO> getRequests(Pageable pageable, String status, String requestType) {
        String statusFilter = (status != null && !status.isBlank()) ? status : null;
        String typeFilter = (requestType != null && !requestType.isBlank()) ? requestType : null;
        return requestRepository.findWithFilters(statusFilter, typeFilter, pageable)
                .map(r -> toDTO(r, false));
    }

    public ApprovalRequestDTO getRequestById(UUID id) {
        ApprovalRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Approval request not found: " + id));
        return toDTO(request, true);
    }

    @Transactional
    public ApprovalRequestDTO createRequest(CreateApprovalRequestRequest req, UUID initiatorUserId) {
        List<CreateApprovalRequestRequest.StepDefinition> stepDefs = req.getSteps();
        int totalSteps = (stepDefs != null && !stepDefs.isEmpty()) ? stepDefs.size() : 1;

        ApprovalRequest request = ApprovalRequest.builder()
                .requestType(req.getRequestType())
                .businessKey(req.getBusinessKey())
                .referenceId(req.getReferenceId())
                .status(STATUS_PENDING)
                .initiatorUserId(initiatorUserId)
                .currentStep(1)
                .totalSteps(totalSteps)
                .payload(req.getPayload())
                .build();
        request = requestRepository.save(request);

        if (stepDefs != null && !stepDefs.isEmpty()) {
            for (int i = 0; i < stepDefs.size(); i++) {
                CreateApprovalRequestRequest.StepDefinition def = stepDefs.get(i);
                String stepStatus = (i == 0) ? STATUS_PENDING : STATUS_SKIPPED;
                UUID approverUserId = null;
                try {
                    if (def.getApproverUserId() != null && !def.getApproverUserId().isBlank()) {
                        approverUserId = UUID.fromString(def.getApproverUserId());
                    }
                } catch (IllegalArgumentException ignored) {}

                ApprovalStep step = ApprovalStep.builder()
                        .requestId(request.getId())
                        .stepNumber(i + 1)
                        .stepName(def.getStepName())
                        .approverType(def.getApproverType() != null ? def.getApproverType() : "ROLE")
                        .approverRoleCode(def.getApproverRoleCode())
                        .approverUserId(approverUserId)
                        .status(stepStatus)
                        .build();
                stepRepository.save(step);
            }
        } else {
            ApprovalStep step = ApprovalStep.builder()
                    .requestId(request.getId())
                    .stepNumber(1)
                    .stepName("Approval")
                    .approverType("ROLE")
                    .status(STATUS_PENDING)
                    .build();
            stepRepository.save(step);
        }

        return toDTO(request, true);
    }

    @Transactional
    public ApprovalRequestDTO processStep(UUID requestId, UUID stepId, StepActionRequest action, UUID actorUserId) {
        ApprovalRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Approval request not found: " + requestId));
        ApprovalStep step = stepRepository.findById(stepId)
                .orElseThrow(() -> new RuntimeException("Approval step not found: " + stepId));

        String oldStatus = step.getStatus();
        String newStepStatus = "APPROVE".equalsIgnoreCase(action.getAction()) ? STATUS_APPROVED : STATUS_REJECTED;

        step.setStatus(newStepStatus);
        step.setComments(action.getComments());
        step.setActionBy(actorUserId);
        step.setActionAt(LocalDateTime.now());
        stepRepository.save(step);

        ApprovalHistory history = ApprovalHistory.builder()
                .requestId(requestId)
                .stepId(stepId)
                .action(action.getAction().toUpperCase())
                .performedBy(actorUserId)
                .comments(action.getComments())
                .oldStatus(oldStatus)
                .newStatus(newStepStatus)
                .build();
        historyRepository.save(history);

        if (STATUS_REJECTED.equals(newStepStatus)) {
            request.setStatus(STATUS_REJECTED);
            request.setCompletedAt(LocalDateTime.now());
        } else {
            int nextStepNumber = step.getStepNumber() + 1;
            if (nextStepNumber > request.getTotalSteps()) {
                request.setStatus(STATUS_COMPLETED);
                request.setCompletedAt(LocalDateTime.now());
            } else {
                request.setCurrentStep(nextStepNumber);
                stepRepository.findByRequestIdAndStepNumber(requestId, nextStepNumber)
                        .ifPresent(nextStep -> {
                            nextStep.setStatus(STATUS_PENDING);
                            stepRepository.save(nextStep);
                        });
            }
        }
        requestRepository.save(request);
        return toDTO(request, true);
    }

    private ApprovalRequestDTO toDTO(ApprovalRequest r, boolean includeDetails) {
        ApprovalRequestDTO.ApprovalRequestDTOBuilder builder = ApprovalRequestDTO.builder()
                .id(r.getId())
                .requestType(r.getRequestType())
                .businessKey(r.getBusinessKey())
                .referenceId(r.getReferenceId())
                .status(r.getStatus())
                .initiatorUserId(r.getInitiatorUserId())
                .currentStep(r.getCurrentStep())
                .totalSteps(r.getTotalSteps())
                .payload(r.getPayload())
                .metadata(r.getMetadata())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .completedAt(r.getCompletedAt());

        if (includeDetails) {
            List<ApprovalStep> steps = stepRepository.findByRequestIdOrderByStepNumberAsc(r.getId());
            builder.steps(steps.stream().map(s -> ApprovalRequestDTO.ApprovalStepDTO.builder()
                    .id(s.getId())
                    .stepNumber(s.getStepNumber())
                    .stepName(s.getStepName())
                    .approverType(s.getApproverType())
                    .approverRoleCode(s.getApproverRoleCode())
                    .approverUserId(s.getApproverUserId())
                    .status(s.getStatus())
                    .comments(s.getComments())
                    .actionBy(s.getActionBy())
                    .actionAt(s.getActionAt())
                    .dueDate(s.getDueDate())
                    .build()).collect(Collectors.toList()));

            List<ApprovalHistory> history = historyRepository.findByRequestIdOrderByPerformedAtAsc(r.getId());
            builder.history(history.stream().map(h -> ApprovalRequestDTO.ApprovalHistoryDTO.builder()
                    .id(h.getId())
                    .stepId(h.getStepId())
                    .action(h.getAction())
                    .performedBy(h.getPerformedBy())
                    .comments(h.getComments())
                    .oldStatus(h.getOldStatus())
                    .newStatus(h.getNewStatus())
                    .performedAt(h.getPerformedAt())
                    .build()).collect(Collectors.toList()));
        }

        return builder.build();
    }
}
