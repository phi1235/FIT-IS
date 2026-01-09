package com.example.ticket.dto;

/**
 * Ticket status enum for the maker-checker workflow
 */
public enum TicketStatus {
    DRAFT,      // Ticket created but not submitted
    PENDING,    // Submitted and waiting for approval
    APPROVED,   // Approved by checker
    REJECTED,   // Rejected by checker
    CLOSED      // Closed after completion
}
