package com.example.report.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Report Job model for tracking async report generation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportJob {
    
    private String jobId;
    private String format;
    private String reportType;
    private Status status;
    private int progress;
    private String fileName;
    private String filePath;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public enum Status {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}
