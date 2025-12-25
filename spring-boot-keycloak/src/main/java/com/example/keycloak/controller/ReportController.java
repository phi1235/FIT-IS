package com.example.keycloak.controller;

import com.example.keycloak.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

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
            return ResponseEntity.internalServerError().build();
        }
    }

    @FunctionalInterface
    interface ReportSupplier {
        byte[] get() throws Exception;
    }
}
