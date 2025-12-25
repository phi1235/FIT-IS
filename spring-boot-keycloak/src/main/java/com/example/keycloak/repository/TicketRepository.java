package com.example.keycloak.repository;

import com.example.keycloak.entity.Ticket;
import com.example.keycloak.dto.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByMaker(String maker);

    List<Ticket> findByStatus(TicketStatus status);
}
