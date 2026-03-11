package com.example.ticket.service;

import com.example.ticket.dto.TicketDTO;
import com.example.ticket.dto.TicketRequest;
import com.example.ticket.dto.TicketStatus;
import com.example.ticket.entity.Priority;
import com.example.ticket.entity.Ticket;
import com.example.ticket.repository.PriorityRepository;
import com.example.ticket.repository.TicketRepository;
import com.example.ticket.repository.TicketUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
    private final PriorityRepository priorityRepository;
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
        TicketStatus initialStatus = request.isSaveDraft() ? TicketStatus.DRAFT : TicketStatus.PENDING;

        // Compute SLA deadline from priority
        LocalDateTime slaDeadline = null;
        if (request.getPriorityId() != null) {
            slaDeadline = priorityRepository.findById(request.getPriorityId())
                    .filter(p -> p.getSlaDurationHours() != null)
                    .map(p -> LocalDateTime.now().plusHours(p.getSlaDurationHours()))
                    .orElse(null);
        }

        Ticket ticket = Ticket.builder()
                .code(code)
                .title(request.getTitle())
                .description(request.getDescription())
                .amount(request.getAmount())
                .status(initialStatus)
                .makerUserId(makerUserId)
                .priorityId(request.getPriorityId())
                .categoryId(request.getCategoryId())
                .slaDeadline(slaDeadline)
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket created: {} by maker {}, SLA deadline: {}", code, makerUserId, slaDeadline);

        return convertToDTO(savedTicket);
    }

    @Transactional
    public TicketDTO updateTicket(UUID id, TicketRequest request, UUID makerUserId) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        if (!ticket.getMakerUserId().equals(makerUserId)) {
            throw new RuntimeException("Only the maker can update the ticket");
        }

        if (ticket.getStatus() != TicketStatus.DRAFT && ticket.getStatus() != TicketStatus.REJECTED) {
            throw new RuntimeException("Only DRAFT or REJECTED tickets can be updated");
        }

        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setAmount(request.getAmount());
        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket updated: {} by maker {}", savedTicket.getCode(), makerUserId);

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

        String priorityCode = null;
        String priorityName = null;
        if (ticket.getPriorityId() != null) {
            Priority priority = priorityRepository.findById(ticket.getPriorityId()).orElse(null);
            if (priority != null) {
                priorityCode = priority.getCode();
                priorityName = priority.getName();
            }
        }

        String slaStatus = computeSlaStatus(ticket);
        Long slaRemainingMinutes = null;
        if (ticket.getSlaDeadline() != null) {
            slaRemainingMinutes = ChronoUnit.MINUTES.between(LocalDateTime.now(), ticket.getSlaDeadline());
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
                .priorityCode(priorityCode)
                .priorityName(priorityName)
                .slaDeadline(ticket.getSlaDeadline())
                .slaStatus(slaStatus)
                .slaRemainingMinutes(slaRemainingMinutes)
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    private String computeSlaStatus(Ticket ticket) {
        if (ticket.getSlaDeadline() == null) return null;
        if (ticket.getStatus() == TicketStatus.APPROVED || ticket.getStatus() == TicketStatus.REJECTED) {
            return "COMPLETED";
        }
        LocalDateTime now = LocalDateTime.now();
        long minutesLeft = ChronoUnit.MINUTES.between(now, ticket.getSlaDeadline());
        if (minutesLeft < 0)  return "BREACHED";
        if (minutesLeft < 60) return "WARNING";  // < 1 hour
        return "ON_TIME";
    }

    /** Chạy mỗi 15 phút — log các ticket PENDING đã vượt SLA */
    @Scheduled(fixedRate = 900_000)
    public void checkSlaBreaches() {
        List<Ticket> overdue = ticketRepository.findOverduePendingTickets(LocalDateTime.now());
        if (!overdue.isEmpty()) {
            log.warn("SLA BREACH: {} PENDING ticket(s) overdue: {}",
                    overdue.size(),
                    overdue.stream().map(Ticket::getCode).collect(Collectors.joining(", ")));
        }
    }

    private String generateTicketCode() {
        String yyyymm = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        int rand = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "TCK-" + yyyymm + "-" + rand;
    }
}
