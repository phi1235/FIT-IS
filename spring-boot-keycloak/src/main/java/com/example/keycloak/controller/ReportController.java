package com.example.keycloak.controller;

import com.example.keycloak.model.ReportJob;
import com.example.keycloak.service.ReportJobService;
import com.example.keycloak.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportJobService jobService;

    // ========== SYNC ENDPOINTS (giữ lại cho backward compatibility) ==========

    @GetMapping("/users")
    public ResponseEntity<byte[]> downloadUserReport(@RequestParam String format) {
        return processReport(format, "users_report", () -> reportService.exportReport(format));
    }

    @GetMapping("/tickets")
    public ResponseEntity<byte[]> downloadTicketReport(@RequestParam String format) {
        return processReport(format, "tickets_report", () -> reportService.exportTicketReport(format));
    }

    private ResponseEntity<byte[]> processReport(String format, String baseFileName, ReportSupplier supplier) {
        try {
            byte[] data = supplier.get();

            String fileName = baseFileName + "." + format.toLowerCase();
            MediaType mediaType = format.equalsIgnoreCase("pdf")
                    ? MediaType.APPLICATION_PDF
                    : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                    .contentType(mediaType)
                    .body(data);

        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = "Report generation failed: " + e.getMessage();
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(errorMsg.getBytes());
        }
    }

    // ========== ASYNC ENDPOINTS ==========

    /**
     * Bắt đầu generate report (async)
     * POST /api/reports/users/generate?format=pdf
     */
    @PostMapping("/{reportType}/generate")
    public ResponseEntity<Map<String, Object>> generateReport(
            @PathVariable String reportType,
            @RequestParam String format) {

        // Validate
        if (!reportType.equals("users") && !reportType.equals("tickets")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid report type"));
        }
        if (!format.equalsIgnoreCase("pdf") && !format.equalsIgnoreCase("xlsx")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid format"));
        }

        // Create job
        ReportJob job = jobService.createJob(format.toLowerCase(), reportType);

        // Start async generation
        reportService.exportReportAsync(job.getJobId(), format.toLowerCase(), reportType);

        // Return job info
        Map<String, Object> response = new HashMap<>();
        response.put("jobId", job.getJobId());
        response.put("status", job.getStatus().name());
        response.put("message", "Report generation started");

        return ResponseEntity.ok(response);
    }

    /**
     * Kiểm tra status của job
     * GET /api/reports/status/{jobId}
     */
    @GetMapping("/status/{jobId}")
    public ResponseEntity<Map<String, Object>> getJobStatus(@PathVariable String jobId) {        ReportJob job = jobService.getJob(jobId);

        if (job == null) {            return ResponseEntity.notFound().build();
        }        Map<String, Object> response = new HashMap<>();
        response.put("jobId", job.getJobId());
        response.put("status", job.getStatus().name());
        response.put("progress", job.getProgress());
        response.put("format", job.getFormat());
        response.put("reportType", job.getReportType());

        if (job.getStatus() == ReportJob.Status.COMPLETED) {
            response.put("downloadUrl", "/api/reports/download/" + jobId);
            response.put("fileName", job.getFileName());
        }

        if (job.getStatus() == ReportJob.Status.FAILED) {
            response.put("errorMessage", job.getErrorMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Download file đã generate xong
     * GET /api/reports/download/{jobId}
     */
    @GetMapping("/download/{jobId}")
    public ResponseEntity<Resource> downloadReport(@PathVariable String jobId) {
        ReportJob job = jobService.getJob(jobId);

        if (job == null) {
            return ResponseEntity.notFound().build();
        }

        if (job.getStatus() != ReportJob.Status.COMPLETED) {
            return ResponseEntity.badRequest().build();
        }

        File file = new File(job.getFilePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);

        MediaType mediaType = job.getFormat().equalsIgnoreCase("pdf")
                ? MediaType.APPLICATION_PDF
                : MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + job.getFileName())
                .contentType(mediaType)
                .contentLength(file.length())
                .body(resource);
    }

    @FunctionalInterface
    interface ReportSupplier {
        byte[] get() throws Exception;
    }
}
