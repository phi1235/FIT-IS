package com.example.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Auth Service - Handles authentication, authorization, and RBAC management
 * 
 * Features:
 * - JWT token generation and validation
 * - User authentication (database, federation)
 * - Role CRUD operations
 * - Permission CRUD operations
 * - Role-Permission mapping
 * - User-Role assignment
 */
@SpringBootApplication(scanBasePackages = "com.example")
@EnableAsync
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
