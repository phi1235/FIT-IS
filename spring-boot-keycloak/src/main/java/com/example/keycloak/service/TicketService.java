package com.example.keycloak.service;

import com.example.keycloak.dto.TicketDTO;
import com.example.keycloak.dto.TicketRequest;
import com.example.keycloak.dto.TicketStatus;
import com.example.keycloak.entity.AuditLog;
import com.example.keycloak.entity.Ticket;
import com.example.keycloak.repository.AuditLogRepository;
import com.example.keycloak.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final AuditLogRepository auditLogRepository;

    public List<TicketDTO> getAllTickets() {
        return ticketRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<TicketDTO> getTicketsByMaker(String username) {
        return ticketRepository.findByMaker(username).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<TicketDTO> getTicketsByStatus(TicketStatus status) {
        return ticketRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public TicketDTO getTicketById(Long id) {
        return ticketRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + id));
    }

    @Transactional
    public TicketDTO createTicket(TicketRequest request, String username) {
        Ticket ticket = Ticket.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .amount(request.getAmount())
                .status(TicketStatus.DRAFT)
                .maker(username)
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);
        logAudit("CREATE", savedTicket.getId().toString(), username, "Created ticket in DRAFT status");
        return convertToDTO(savedTicket);
    }

    @Transactional
    public TicketDTO submitTicket(Long id, String username) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        if (!ticket.getMaker().equals(username)) {
            throw new RuntimeException("Only the maker can submit the ticket");
        }

        if (ticket.getStatus() != TicketStatus.DRAFT && ticket.getStatus() != TicketStatus.REJECTED) {
            throw new RuntimeException("Only DRAFT or REJECTED tickets can be submitted");
        }

        ticket.setStatus(TicketStatus.SUBMITTED);
        Ticket savedTicket = ticketRepository.save(ticket);
        logAudit("SUBMIT", savedTicket.getId().toString(), username, "Submitted ticket for approval");
        return convertToDTO(savedTicket);
    }

    @Transactional
    public TicketDTO approveTicket(Long id, String checkerUsername) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        if (ticket.getMaker().equals(checkerUsername)) {
            throw new RuntimeException("Maker and Checker must be different");
        }

        if (ticket.getStatus() != TicketStatus.SUBMITTED) {
            throw new RuntimeException("Only SUBMITTED tickets can be approved");
        }

        ticket.setStatus(TicketStatus.APPROVED);
        ticket.setChecker(checkerUsername);
        Ticket savedTicket = ticketRepository.save(ticket);
        logAudit("APPROVE", savedTicket.getId().toString(), checkerUsername, "Approved ticket");
        return convertToDTO(savedTicket);
    }

    @Transactional
    public TicketDTO rejectTicket(Long id, String checkerUsername, String reason) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        if (ticket.getMaker().equals(checkerUsername)) {
            throw new RuntimeException("Maker and Checker must be different");
        }

        if (ticket.getStatus() != TicketStatus.SUBMITTED) {
            throw new RuntimeException("Only SUBMITTED tickets can be rejected");
        }

        ticket.setStatus(TicketStatus.REJECTED);
        ticket.setChecker(checkerUsername);
        ticket.setRejectionReason(reason);
        Ticket savedTicket = ticketRepository.save(ticket);
        logAudit("REJECT", savedTicket.getId().toString(), checkerUsername, "Rejected ticket with reason: " + reason);
        return convertToDTO(savedTicket);
    }

    private void logAudit(String action, String entityId, String userId, String details) {
        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .entityType("TICKET")
                .entityId(entityId)
                .userId(userId)
                .details(details)
                .build();
        auditLogRepository.save(auditLog);
    }

    private TicketDTO convertToDTO(Ticket ticket) {
        return TicketDTO.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .amount(ticket.getAmount())
                .maker(ticket.getMaker())
                .checker(ticket.getChecker())
                .rejectionReason(ticket.getRejectionReason())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }
}
