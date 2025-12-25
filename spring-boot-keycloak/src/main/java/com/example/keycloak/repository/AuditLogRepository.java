package com.example.keycloak.repository;

import com.example.keycloak.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUserId(String userId);

    List<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId);
}
