package com.example.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all domain events in the microservices architecture.
 * All events should extend this class to ensure consistent event structure.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent {
    
    private UUID eventId;
    private LocalDateTime timestamp;
    private UUID userId;
    private String username;
    private String sourceIp;
    private String userAgent;
    
    /**
     * Get the event type name (used for routing and logging)
     */
    public abstract String getEventType();
}
