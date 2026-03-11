package com.example.ticket.repository;

import com.example.ticket.entity.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PriorityRepository extends JpaRepository<Priority, UUID> {
}
