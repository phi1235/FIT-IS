package com.example.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Event emitted when a ticket is created
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TicketCreatedEvent extends BaseEvent {
    
    private UUID ticketId;
    private String ticketCode;
    private String title;
    private UUID makerUserId;
    
    @Override
    public String getEventType() {
        return "TICKET_CREATED";
    }
}
