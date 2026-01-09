package com.example.ticket.repository;

import com.example.ticket.dto.TicketStatus;
import com.example.ticket.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findByMakerUserId(UUID makerUserId);

    List<Ticket> findByStatus(TicketStatus status);

    Page<Ticket> findByMakerUserId(UUID makerUserId, Pageable pageable);

    @Query("SELECT t FROM Ticket t WHERE " +
            "LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(t.code) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Ticket> searchTickets(@Param("search") String search, Pageable pageable);

    @Query("SELECT t FROM Ticket t WHERE t.makerUserId = :makerUserId AND (" +
            "LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(t.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Ticket> searchTicketsByMaker(@Param("makerUserId") UUID makerUserId,
                                      @Param("search") String search,
                                      Pageable pageable);

    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

    Page<Ticket> findByMakerUserIdAndStatus(UUID makerUserId, TicketStatus status, Pageable pageable);

    @Query("SELECT t FROM Ticket t WHERE t.status = :status AND (" +
            "LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(t.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Ticket> searchTicketsByStatus(@Param("status") TicketStatus status,
                                       @Param("search") String search,
                                       Pageable pageable);

    @Query("SELECT t FROM Ticket t WHERE t.makerUserId = :makerUserId AND t.status = :status AND (" +
            "LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(t.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Ticket> searchTicketsByMakerAndStatus(@Param("makerUserId") UUID makerUserId,
                                               @Param("status") TicketStatus status,
                                               @Param("search") String search,
                                               Pageable pageable);
}
