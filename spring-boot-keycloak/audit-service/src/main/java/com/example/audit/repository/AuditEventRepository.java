package com.example.audit.repository;

import com.example.audit.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    @Query("SELECT a FROM AuditEvent a WHERE " +
            "(:eventType IS NULL OR a.eventType = :eventType) AND " +
            "(:username IS NULL OR LOWER(a.username) LIKE LOWER(CONCAT('%', :username, '%'))) AND " +
            "a.eventTime >= :fromDate AND " +
            "a.eventTime <= :toDate " +
            "ORDER BY a.eventTime DESC")
    Page<AuditEvent> findWithFilters(
            @Param("eventType") String eventType,
            @Param("username") String username,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);
}
