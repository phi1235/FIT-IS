package com.example.ticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Ticket Service - Handles ticket CRUD and maker-checker workflow
 * 
 * Features:
 * - Ticket CRUD operations
 * - Status lifecycle (DRAFT → PENDING → APPROVED/REJECTED)
 * - Maker-Checker workflow
 * - Event publishing for audit
 */
@SpringBootApplication(scanBasePackages = "com.example")
@EnableAsync
public class TicketServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketServiceApplication.class, args);
    }
}
