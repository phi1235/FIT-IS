package com.example.keycloak.event;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Event khi ticket được tạo mới
 */
@Getter
public class TicketCreatedEvent extends BaseEvent {
    
    private final Long ticketId;
    private final String title;
    private final BigDecimal amount;
    private final String maker;
    
    public TicketCreatedEvent(Object source, Long ticketId, String title, 
                              BigDecimal amount, String maker) {
        super(source, "TICKET_CREATED", maker);
        this.ticketId = ticketId;
        this.title = title;
        this.amount = amount;
        this.maker = maker;
    }
}
