package com.example.ticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

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
@EnableScheduling
public class TicketServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketServiceApplication.class, args);
    }
}
