package com.example.workflow.repository;

import com.example.workflow.entity.ApprovalStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, UUID> {

    List<ApprovalStep> findByRequestIdOrderByStepNumberAsc(UUID requestId);

    Optional<ApprovalStep> findByRequestIdAndStepNumber(UUID requestId, Integer stepNumber);
}
