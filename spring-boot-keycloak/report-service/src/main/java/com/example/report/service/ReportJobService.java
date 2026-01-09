package com.example.report.service;

import com.example.report.model.ReportJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing report job tracking
 */
@Slf4j
@Service
public class ReportJobService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String JOB_PREFIX = "report:job:";
    private static final long JOB_TTL_HOURS = 24;

    public ReportJobService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public ReportJob createJob(String format, String reportType) {
        String jobId = UUID.randomUUID().toString();
        ReportJob job = ReportJob.builder()
                .jobId(jobId)
                .format(format)
                .reportType(reportType)
                .status(ReportJob.Status.PENDING)
                .progress(0)
                .createdAt(LocalDateTime.now())
                .build();

        saveJob(job);
        log.info("Created report job: {} for type: {} format: {}", jobId, reportType, format);
        return job;
    }

    public void updateProgress(String jobId, int progress) {
        ReportJob job = getJob(jobId);
        if (job != null) {
            job.setProgress(progress);
            job.setStatus(ReportJob.Status.PROCESSING);
            saveJob(job);
        }
    }

    public void markCompleted(String jobId, String fileName, String filePath) {
        ReportJob job = getJob(jobId);
        if (job != null) {
            job.setStatus(ReportJob.Status.COMPLETED);
            job.setProgress(100);
            job.setFileName(fileName);
            job.setFilePath(filePath);
            job.setCompletedAt(LocalDateTime.now());
            saveJob(job);
            log.info("Report job completed: {} - file: {}", jobId, fileName);
        }
    }

    public void markFailed(String jobId, String errorMessage) {
        ReportJob job = getJob(jobId);
        if (job != null) {
            job.setStatus(ReportJob.Status.FAILED);
            job.setErrorMessage(errorMessage);
            job.setCompletedAt(LocalDateTime.now());
            saveJob(job);
            log.error("Report job failed: {} - error: {}", jobId, errorMessage);
        }
    }

    public ReportJob getJob(String jobId) {
        return (ReportJob) redisTemplate.opsForValue().get(JOB_PREFIX + jobId);
    }

    private void saveJob(ReportJob job) {
        redisTemplate.opsForValue().set(JOB_PREFIX + job.getJobId(), job, JOB_TTL_HOURS, TimeUnit.HOURS);
    }
}
