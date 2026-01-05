package com.example.keycloak.controller;

import com.example.keycloak.dto.*;
import com.example.keycloak.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Validated
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<List<TicketDTO>> getAllTickets(Authentication authentication) {
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_admin") || a.getAuthority().equals("ROLE_checker"))) {
            return ResponseEntity.ok(ticketService.getAllTickets());
        }
        return ResponseEntity.ok(ticketService.getTicketsByMaker(authentication.getName()));
    }

    @GetMapping("/paginated")
    public ResponseEntity<Page<TicketDTO>> getAllTicketsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) TicketStatus status,
            Authentication authentication) {

        System.out.println("========== DEBUG /paginated ==========");
        System.out.println("page=" + page + ", size=" + size + ", search=" + search + ", status=" + status);
        System.out.println("======================================");

        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_admin") || a.getAuthority().equals("ROLE_checker"))) {
            return ResponseEntity.ok(ticketService.getAllTicketsPaginatedWithStatus(page, size, search, status));
        }
        return ResponseEntity
                .ok(ticketService.getTicketsByMakerPaginatedWithStatus(authentication.getName(), page, size, search, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketDTO> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<TicketDTO>> getTicketsByStatus(@PathVariable TicketStatus status) {
        return ResponseEntity.ok(ticketService.getTicketsByStatus(status));
    }

    @PostMapping
    @PreAuthorize("hasRole('maker') or hasRole('admin') or hasRole('user')")
    public ResponseEntity<TicketDTO> createTicket(@Valid @RequestBody TicketRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ticketService.createTicket(request, authentication.getName()));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('maker') or hasRole('admin') or hasRole('user')")
    public ResponseEntity<TicketDTO> submitTicket(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ticketService.submitTicket(id, authentication.getName()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('checker') or hasRole('admin')")
    public ResponseEntity<TicketDTO> approveTicket(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ticketService.approveTicket(id, authentication.getName()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('checker') or hasRole('admin')")
    public ResponseEntity<TicketDTO> rejectTicket(@PathVariable Long id, @Valid @RequestBody RejectionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ticketService.rejectTicket(id, authentication.getName(), request.getReason()));
    }
}
