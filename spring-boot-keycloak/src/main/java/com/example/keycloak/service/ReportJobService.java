package com.example.keycloak.service;

import com.example.keycloak.model.ReportJob;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service để quản lý report generation jobs
 * Sử dụng in-memory storage (ConcurrentHashMap)
 * Production nên chuyển sang Redis
 */
@Service
public class ReportJobService {

    // Thư mục lưu file report tạm
    private static final String REPORT_DIR = "/tmp/reports";

    // In-memory job storage
    private final Map<String, ReportJob> jobs = new ConcurrentHashMap<>();

    /**
     * Tạo job mới
     */
    public ReportJob createJob(String format, String reportType) {
        String jobId = UUID.randomUUID().toString();
        ReportJob job = new ReportJob(jobId, format, reportType);
        jobs.put(jobId, job);

        // Tạo thư mục nếu chưa có
        new File(REPORT_DIR).mkdirs();

        return job;
    }

    /**
     * Lấy job theo ID
     */
    public ReportJob getJob(String jobId) {
        return jobs.get(jobId);
    }

    /**
     * Update progress
     */
    public void updateProgress(String jobId, int progress) {
        ReportJob job = jobs.get(jobId);
        if (job != null) {
            job.setProgress(progress);
            job.setStatus(ReportJob.Status.PROCESSING);
        }
    }

    /**
     * Đánh dấu job hoàn thành
     */
    public void markCompleted(String jobId, String filePath, String fileName) {
        ReportJob job = jobs.get(jobId);
        if (job != null) {
            job.setStatus(ReportJob.Status.COMPLETED);
            job.setProgress(100);
            job.setFilePath(filePath);
            job.setFileName(fileName);
            job.setCompletedAt(LocalDateTime.now());
        }
    }

    /**
     * Đánh dấu job thất bại
     */
    public void markFailed(String jobId, String errorMessage) {
        ReportJob job = jobs.get(jobId);
        if (job != null) {
            job.setStatus(ReportJob.Status.FAILED);
            job.setErrorMessage(errorMessage);
            job.setCompletedAt(LocalDateTime.now());
        }
    }

    /**
     * Lấy đường dẫn file
     */
    public String getFilePath(String jobId, String format) {
        return REPORT_DIR + "/" + jobId + "." + format;
    }

    /**
     * Xóa job và file (cleanup)
     */
    public void deleteJob(String jobId) {
        ReportJob job = jobs.remove(jobId);
        if (job != null && job.getFilePath() != null) {
            new File(job.getFilePath()).delete();
        }
    }
}
