package com.example.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Event emitted when a ticket status changes (submitted, approved, rejected)
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TicketStatusChangedEvent extends BaseEvent {
    
    private UUID ticketId;
    private String ticketCode;
    private String previousStatus;
    private String newStatus;
    private UUID changedByUserId;
    private String reason;
    
    @Override
    public String getEventType() {
        return "TICKET_STATUS_CHANGED";
    }
}
