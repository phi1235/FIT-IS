package com.example.audit.controller;

import com.example.audit.dto.AuditEventDTO;
import com.example.audit.dto.CreateAuditEventRequest;
import com.example.audit.service.AuditEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditEventService auditEventService;

    @GetMapping("/events")
    @PreAuthorize("hasAuthority('AUDIT_VIEW')")
    public ResponseEntity<Page<AuditEventDTO>> getEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {
        return ResponseEntity.ok(auditEventService.getEvents(PageRequest.of(page, size), eventType, username, fromDate, toDate));
    }

    @PostMapping("/events")
    public ResponseEntity<AuditEventDTO> createEvent(@RequestBody CreateAuditEventRequest request) {
        return ResponseEntity.ok(auditEventService.createEvent(request));
    }
}
