package com.example.workflow.repository;

import com.example.workflow.entity.ApprovalRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {

    @Query("SELECT r FROM ApprovalRequest r WHERE " +
            "(:status IS NULL OR r.status = :status) AND " +
            "(:requestType IS NULL OR r.requestType = :requestType) " +
            "ORDER BY r.createdAt DESC")
    Page<ApprovalRequest> findWithFilters(
            @Param("status") String status,
            @Param("requestType") String requestType,
            Pageable pageable);
}
