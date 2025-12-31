package com.example.keycloak.event;

import com.example.keycloak.dto.TicketStatus;
import lombok.Getter;

/**
 * Event khi status của ticket thay đổi (submit, approve, reject)
 */
@Getter
public class TicketStatusChangedEvent extends BaseEvent {
    
    private final Long ticketId;
    private final TicketStatus previousStatus;
    private final TicketStatus newStatus;
    private final String changedBy;
    private final String reason; // Cho trường hợp reject
    
    public TicketStatusChangedEvent(Object source, Long ticketId, 
                                    TicketStatus previousStatus, TicketStatus newStatus,
                                    String changedBy, String reason) {
        super(source, "TICKET_STATUS_CHANGED", changedBy);
        this.ticketId = ticketId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.reason = reason;
    }
    
    public TicketStatusChangedEvent(Object source, Long ticketId, 
                                    TicketStatus previousStatus, TicketStatus newStatus,
                                    String changedBy) {
        this(source, ticketId, previousStatus, newStatus, changedBy, null);
    }
}
