package com.example.audit.controller;

import com.example.audit.dto.CreateNotificationRequest;
import com.example.audit.dto.NotificationDTO;
import com.example.audit.entity.SystemNotification;
import com.example.audit.repository.SystemNotificationRepository;
import com.example.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final SystemNotificationRepository notificationRepository;

    /** Lấy thông báo đang active — tất cả user đã đăng nhập */
    @GetMapping("/active")
    public ResponseEntity<List<NotificationDTO>> getActiveNotifications() {
        List<NotificationDTO> list = notificationRepository
                .findActiveNotifications(LocalDateTime.now())
                .stream()
                .map(NotificationDTO::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    /** Admin: lấy tất cả thông báo (có phân trang) */
    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_VIEW')")
    public ResponseEntity<Page<NotificationDTO>> getAllNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<NotificationDTO> result = notificationRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(NotificationDTO::from);
        return ResponseEntity.ok(result);
    }

    /** Admin: tạo thông báo mới */
    @PostMapping
    @PreAuthorize("hasAuthority('AUDIT_VIEW')")
    @Transactional
    public ResponseEntity<NotificationDTO> createNotification(@RequestBody CreateNotificationRequest req) {
        String creator = SecurityUtils.getCurrentUsername();
        SystemNotification notification = SystemNotification.builder()
                .title(req.getTitle())
                .content(req.getContent())
                .type(req.getType() != null ? req.getType() : "info")
                .createdBy(creator != null ? creator : "system")
                .active(true)
                .expiresAt(req.getExpiresAt())
                .build();
        notification = notificationRepository.save(notification);
        return ResponseEntity.ok(NotificationDTO.from(notification));
    }

    /** Admin: cập nhật thông báo */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('AUDIT_VIEW')")
    @Transactional
    public ResponseEntity<NotificationDTO> updateNotification(
            @PathVariable UUID id,
            @RequestBody CreateNotificationRequest req) {
        return notificationRepository.findById(id)
                .map(n -> {
                    if (req.getTitle() != null) n.setTitle(req.getTitle());
                    if (req.getContent() != null) n.setContent(req.getContent());
                    if (req.getType() != null) n.setType(req.getType());
                    n.setExpiresAt(req.getExpiresAt());
                    return ResponseEntity.ok(NotificationDTO.from(notificationRepository.save(n)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Admin: bật/tắt thông báo */
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('AUDIT_VIEW')")
    @Transactional
    public ResponseEntity<NotificationDTO> toggleNotification(@PathVariable UUID id) {
        return notificationRepository.findById(id)
                .map(n -> {
                    n.setActive(!n.isActive());
                    return ResponseEntity.ok(NotificationDTO.from(notificationRepository.save(n)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Admin: xóa thông báo */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('AUDIT_VIEW')")
    @Transactional
    public ResponseEntity<Void> deleteNotification(@PathVariable UUID id) {
        if (!notificationRepository.existsById(id)) return ResponseEntity.notFound().build();
        notificationRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
