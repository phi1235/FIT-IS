package com.example.audit.repository;

import com.example.audit.entity.SystemNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SystemNotificationRepository extends JpaRepository<SystemNotification, UUID> {

    @Query("SELECT n FROM SystemNotification n WHERE n.active = true " +
           "AND (n.expiresAt IS NULL OR n.expiresAt > :now) " +
           "ORDER BY n.createdAt DESC")
    List<SystemNotification> findActiveNotifications(LocalDateTime now);

    Page<SystemNotification> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
