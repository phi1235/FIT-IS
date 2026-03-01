package com.example.audit.service;

import com.example.audit.dto.AuditEventDTO;
import com.example.audit.dto.CreateAuditEventRequest;
import com.example.audit.entity.AuditEvent;
import com.example.audit.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditEventService {

    private final AuditEventRepository repository;

    public Page<AuditEventDTO> getEvents(Pageable pageable, String eventType, String username,
                                          LocalDateTime fromDate, LocalDateTime toDate) {
        String eventTypeFilter = (eventType != null && !eventType.isBlank()) ? eventType : null;
        String usernameFilter = (username != null && !username.isBlank()) ? username : null;
        LocalDateTime from = fromDate != null ? fromDate : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime to = toDate != null ? toDate : LocalDateTime.of(2099, 12, 31, 23, 59);
        return repository.findWithFilters(eventTypeFilter, usernameFilter, from, to, pageable)
                .map(this::toDTO);
    }

    public AuditEventDTO createEvent(CreateAuditEventRequest request) {
        AuditEvent entity = AuditEvent.builder()
                .eventType(request.getEventType())
                .eventTime(request.getEventTime() != null ? request.getEventTime() : LocalDateTime.now())
                .userId(request.getUserId())
                .username(request.getUsername())
                .sourceIp(request.getSourceIp())
                .userAgent(request.getUserAgent())
                .httpMethod(request.getHttpMethod())
                .requestUrl(request.getRequestUrl())
                .statusCode(request.getStatusCode())
                .errorMessage(request.getErrorMessage())
                .metadata(request.getMetadata())
                .build();
        return toDTO(repository.save(entity));
    }

    private AuditEventDTO toDTO(AuditEvent e) {
        return AuditEventDTO.builder()
                .id(e.getId())
                .eventType(e.getEventType())
                .eventTime(e.getEventTime())
                .userId(e.getUserId())
                .username(e.getUsername())
                .sourceIp(e.getSourceIp())
                .userAgent(e.getUserAgent())
                .httpMethod(e.getHttpMethod())
                .requestUrl(e.getRequestUrl())
                .statusCode(e.getStatusCode())
                .errorMessage(e.getErrorMessage())
                .metadata(e.getMetadata())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
