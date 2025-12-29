package com.example.keycloak.repository;

import com.example.keycloak.entity.Ticket;
import com.example.keycloak.dto.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByMaker(String maker);

    List<Ticket> findByStatus(TicketStatus status);

    // Paginated queries
    Page<Ticket> findAll(Pageable pageable);

    Page<Ticket> findByMaker(String maker, Pageable pageable);

    @Query("SELECT t FROM Ticket t WHERE " +
            "LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(t.maker) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Ticket> searchTickets(@Param("search") String search, Pageable pageable);

    @Query("SELECT t FROM Ticket t WHERE t.maker = :maker AND (" +
            "LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Ticket> searchTicketsByMaker(@Param("maker") String maker, @Param("search") String search, Pageable pageable);
}
