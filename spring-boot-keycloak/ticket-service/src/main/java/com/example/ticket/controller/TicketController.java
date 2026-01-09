package com.example.ticket.controller;

import com.example.ticket.dto.RejectionRequest;
import com.example.ticket.dto.TicketDTO;
import com.example.ticket.dto.TicketRequest;
import com.example.ticket.dto.TicketStatus;
import com.example.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Validated
public class TicketController {

    private final TicketService ticketService;

    private UUID requireUserId(HttpServletRequest request) {
        Object raw = request.getAttribute("userId");
        if (raw == null) {
            throw new RuntimeException("Missing userId in request (JWT not parsed?)");
        }
        try {
            return UUID.fromString(String.valueOf(raw));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid userId in request");
        }
    }

    @GetMapping
    public ResponseEntity<List<TicketDTO>> getAllTickets(Authentication authentication, HttpServletRequest request) {
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_admin") || a.getAuthority().equals("ROLE_checker"))) {
            return ResponseEntity.ok(ticketService.getAllTickets());
        }
        return ResponseEntity.ok(ticketService.getTicketsByMaker(requireUserId(request)));
    }

    @GetMapping("/paginated")
    public ResponseEntity<Page<TicketDTO>> getAllTicketsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) TicketStatus status,
            Authentication authentication,
            HttpServletRequest request) {

        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_admin") || a.getAuthority().equals("ROLE_checker"))) {
            return ResponseEntity.ok(ticketService.getAllTicketsPaginatedWithStatus(page, size, search, status));
        }

        return ResponseEntity.ok(ticketService.getTicketsByMakerPaginatedWithStatus(
                requireUserId(request), page, size, search, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketDTO> getTicketById(@PathVariable UUID id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<TicketDTO>> getTicketsByStatus(@PathVariable TicketStatus status) {
        return ResponseEntity.ok(ticketService.getTicketsByStatus(status));
    }

    @PostMapping
    @PreAuthorize("hasRole('maker') or hasRole('admin') or hasRole('user')")
    public ResponseEntity<TicketDTO> createTicket(
            @Valid @RequestBody TicketRequest request,
            HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(ticketService.createTicket(request, requireUserId(httpServletRequest)));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('maker') or hasRole('admin') or hasRole('user')")
    public ResponseEntity<TicketDTO> submitTicket(@PathVariable UUID id, HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(ticketService.submitTicket(id, requireUserId(httpServletRequest)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('checker') or hasRole('admin')")
    public ResponseEntity<TicketDTO> approveTicket(@PathVariable UUID id, HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(ticketService.approveTicket(id, requireUserId(httpServletRequest)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('checker') or hasRole('admin')")
    public ResponseEntity<TicketDTO> rejectTicket(
            @PathVariable UUID id,
            @Valid @RequestBody RejectionRequest request,
            HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(ticketService.rejectTicket(id, requireUserId(httpServletRequest), request.getReason()));
    }
}
