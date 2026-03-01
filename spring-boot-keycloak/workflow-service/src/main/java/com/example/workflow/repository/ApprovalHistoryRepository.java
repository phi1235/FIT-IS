package com.example.workflow.repository;

import com.example.workflow.entity.ApprovalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, UUID> {

    List<ApprovalHistory> findByRequestIdOrderByPerformedAtAsc(UUID requestId);
}
