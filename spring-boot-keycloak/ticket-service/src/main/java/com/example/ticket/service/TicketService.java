package com.example.ticket.service;

import com.example.ticket.dto.TicketDTO;
import com.example.ticket.dto.TicketRequest;
import com.example.ticket.dto.TicketStatus;
import com.example.ticket.entity.Ticket;
import com.example.ticket.repository.TicketRepository;
import com.example.ticket.repository.TicketUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Ticket Service - handles ticket CRUD and maker-checker workflow
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketUserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public List<TicketDTO> getAllTickets() {
        return ticketRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<TicketDTO> getTicketsByMaker(UUID makerUserId) {
        return ticketRepository.findByMakerUserId(makerUserId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<TicketDTO> getTicketsByStatus(TicketStatus status) {
        return ticketRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Page<TicketDTO> getAllTicketsPaginatedWithStatus(int page, int size, String search, TicketStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Ticket> ticketPage;

        if (status == null) {
            if (search == null || search.trim().isEmpty()) {
                ticketPage = ticketRepository.findAll(pageable);
            } else {
                ticketPage = ticketRepository.searchTickets(search.trim(), pageable);
            }
        } else {
            if (search == null || search.trim().isEmpty()) {
                ticketPage = ticketRepository.findByStatus(status, pageable);
            } else {
                ticketPage = ticketRepository.searchTicketsByStatus(status, search.trim(), pageable);
            }
        }

        return ticketPage.map(this::convertToDTO);
    }

    public Page<TicketDTO> getTicketsByMakerPaginatedWithStatus(UUID makerUserId, int page, int size, String search, TicketStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Ticket> ticketPage;

        if (status == null) {
            if (search == null || search.trim().isEmpty()) {
                ticketPage = ticketRepository.findByMakerUserId(makerUserId, pageable);
            } else {
                ticketPage = ticketRepository.searchTicketsByMaker(makerUserId, search.trim(), pageable);
            }
        } else {
            if (search == null || search.trim().isEmpty()) {
                ticketPage = ticketRepository.findByMakerUserIdAndStatus(makerUserId, status, pageable);
            } else {
                ticketPage = ticketRepository.searchTicketsByMakerAndStatus(makerUserId, status, search.trim(), pageable);
            }
        }

        return ticketPage.map(this::convertToDTO);
    }

    public TicketDTO getTicketById(UUID id) {
        return ticketRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + id));
    }

    @Transactional
    public TicketDTO createTicket(TicketRequest request, UUID makerUserId) {
        String code = generateTicketCode();

        Ticket ticket = Ticket.builder()
                .code(code)
                .title(request.getTitle())
                .description(request.getDescription())
                .amount(request.getAmount())
                .status(TicketStatus.DRAFT)
                .makerUserId(makerUserId)
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket created: {} by maker {}", code, makerUserId);

        return convertToDTO(savedTicket);
    }

    @Transactional
    public TicketDTO submitTicket(UUID id, UUID makerUserId) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        if (!ticket.getMakerUserId().equals(makerUserId)) {
            throw new RuntimeException("Only the maker can submit the ticket");
        }

        if (ticket.getStatus() != TicketStatus.DRAFT && ticket.getStatus() != TicketStatus.REJECTED) {
            throw new RuntimeException("Only DRAFT or REJECTED tickets can be submitted");
        }

        ticket.setStatus(TicketStatus.PENDING);
        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket submitted: {} by maker {}", savedTicket.getCode(), makerUserId);

        return convertToDTO(savedTicket);
    }

    @Transactional
    public TicketDTO approveTicket(UUID id, UUID checkerUserId) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        if (ticket.getMakerUserId().equals(checkerUserId)) {
            throw new RuntimeException("Maker and Checker must be different");
        }

        if (ticket.getStatus() != TicketStatus.PENDING) {
            throw new RuntimeException("Only PENDING tickets can be approved");
        }

        ticket.setStatus(TicketStatus.APPROVED);
        ticket.setCheckerUserId(checkerUserId);
        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket approved: {} by checker {}", savedTicket.getCode(), checkerUserId);

        return convertToDTO(savedTicket);
    }

    @Transactional
    public TicketDTO rejectTicket(UUID id, UUID checkerUserId, String reason) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        if (ticket.getMakerUserId().equals(checkerUserId)) {
            throw new RuntimeException("Maker and Checker must be different");
        }

        if (ticket.getStatus() != TicketStatus.PENDING) {
            throw new RuntimeException("Only PENDING tickets can be rejected");
        }

        ticket.setStatus(TicketStatus.REJECTED);
        ticket.setCheckerUserId(checkerUserId);
        ticket.setRejectionReason(reason);
        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket rejected: {} by checker {} - reason: {}", savedTicket.getCode(), checkerUserId, reason);

        return convertToDTO(savedTicket);
    }

    private TicketDTO convertToDTO(Ticket ticket) {
        String makerName = userRepository.findById(ticket.getMakerUserId())
                .map(u -> u.getFullName())
                .orElse("Unknown (" + ticket.getMakerUserId() + ")");

        String checkerName = null;
        if (ticket.getCheckerUserId() != null) {
            checkerName = userRepository.findById(ticket.getCheckerUserId())
                    .map(u -> u.getFullName())
                    .orElse("Unknown (" + ticket.getCheckerUserId() + ")");
        }

        return TicketDTO.builder()
                .id(ticket.getId())
                .code(ticket.getCode())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus().name())
                .amount(ticket.getAmount())
                .makerUserId(ticket.getMakerUserId())
                .checkerUserId(ticket.getCheckerUserId())
                .makerName(makerName)
                .checkerName(checkerName)
                .rejectionReason(ticket.getRejectionReason())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    private String generateTicketCode() {
        String yyyymm = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        int rand = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "TCK-" + yyyymm + "-" + rand;
    }
}
