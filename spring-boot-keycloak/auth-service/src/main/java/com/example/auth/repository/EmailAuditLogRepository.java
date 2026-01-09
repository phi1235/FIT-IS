package com.example.auth.repository;

import com.example.auth.model.EmailAuditLog;
import com.example.auth.model.EmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmailAuditLogRepository extends JpaRepository<EmailAuditLog, UUID> {
    
    @Query("SELECT eal FROM EmailAuditLog eal WHERE eal.user.id = :userId ORDER BY eal.sentAt DESC")
    List<EmailAuditLog> findByUserId(@Param("userId") UUID userId);
    
    List<EmailAuditLog> findByStatus(EmailStatus status);
    
    @Query("SELECT eal FROM EmailAuditLog eal WHERE eal.templateCode = :templateCode ORDER BY eal.sentAt DESC")
    List<EmailAuditLog> findByTemplateCode(@Param("templateCode") String templateCode);
    
    @Query("SELECT eal FROM EmailAuditLog eal WHERE eal.user.id = :userId AND eal.templateCode = :templateCode ORDER BY eal.sentAt DESC")
    List<EmailAuditLog> findByUserIdAndTemplateCode(@Param("userId") UUID userId, @Param("templateCode") String templateCode);
    
    @Query("SELECT eal FROM EmailAuditLog eal WHERE eal.sentAt BETWEEN :startDate AND :endDate ORDER BY eal.sentAt DESC")
    List<EmailAuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(eal) FROM EmailAuditLog eal WHERE eal.user.id = :userId AND eal.status = :status AND eal.sentAt >= :since")
    long countFailedEmailsInTimeWindow(@Param("userId") UUID userId, @Param("status") EmailStatus status, @Param("since") LocalDateTime since);
}
