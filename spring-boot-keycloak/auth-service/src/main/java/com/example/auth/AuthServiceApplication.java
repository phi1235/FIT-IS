package com.example.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import com.example.auth.service.RoleService;

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
@Slf4j
@SpringBootApplication(scanBasePackages = "com.example")
@EnableAsync
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner initAdminPermissions(RoleService roleService) {
        return args -> {
            try {
                log.info("Starting automatic admin permission synchronization...");
                roleService.syncAdminPermissions();
                log.info("Admin permissions synchronized successfully");
            } catch (Exception e) {
                log.error("Failed to sync admin permissions on startup", e);
            }
        };
    }
}
