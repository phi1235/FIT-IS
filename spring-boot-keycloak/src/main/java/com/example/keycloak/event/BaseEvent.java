package com.example.keycloak.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Base event class cho tất cả domain events
 * Sử dụng Spring ApplicationEvent để tích hợp với Spring Event system
 */
@Getter
public abstract class BaseEvent extends ApplicationEvent {
    
    private final String eventType;
    private final String userId;
    private final LocalDateTime eventTime; // Renamed from timestamp to avoid conflict with ApplicationEvent
    private final String ipAddress;
    
    protected BaseEvent(Object source, String eventType, String userId, String ipAddress) {
        super(source);
        this.eventType = eventType;
        this.userId = userId;
        this.eventTime = LocalDateTime.now();
        this.ipAddress = ipAddress;
    }
    
    protected BaseEvent(Object source, String eventType, String userId) {
        this(source, eventType, userId, null);
    }
}
