package com.example.ticket.repository;

import com.example.ticket.entity.TicketUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TicketUserRepository extends JpaRepository<TicketUser, UUID> {
}
