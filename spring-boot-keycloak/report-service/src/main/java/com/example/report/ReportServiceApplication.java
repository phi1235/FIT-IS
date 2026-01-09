package com.example.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Report Service - Handles JasperReports generation
 * 
 * Features:
 * - Async report generation
 * - PDF/XLSX export
 * - Job tracking and status
 * - Template management
 */
@SpringBootApplication(scanBasePackages = "com.example")
@EnableAsync
public class ReportServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportServiceApplication.class, args);
    }
}
