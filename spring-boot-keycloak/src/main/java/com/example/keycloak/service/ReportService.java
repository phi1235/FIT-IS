package com.example.keycloak.service;

import com.example.keycloak.model.ReportJob;
import com.example.keycloak.provider.CustomUserRepository;
import com.example.keycloak.repository.TicketRepository;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    @Value("${jasperreports.template-path}")
    private String templatePath;

    @Value("${jasperreports.templates.users}")
    private String usersTemplate;

    @Value("${jasperreports.templates.tickets}")
    private String ticketsTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ReportJobService jobService;

    @Autowired
    private TicketRepository ticketRepository;

    /**
     * Load template từ Jaspersoft workspace bên ngoài
     */
    public byte[] exportReport(String format) throws Exception {
        return exportReportFromExternal(format, usersTemplate, "users");
    }

    public byte[] exportTicketReport(String format) throws Exception {
        return exportReportFromExternal(format, ticketsTemplate, "tickets");
    }

    private byte[] exportReportFromExternal(String format, String templateFileName, String reportType)
            throws Exception {
        // Load template từ external Jaspersoft workspace
        File templateFile = new File(templatePath, templateFileName);
        if (!templateFile.exists()) {
            throw new RuntimeException("Report template not found at: " + templateFile.getAbsolutePath());
        }

        System.out.println("Loading report template from: " + templateFile.getAbsolutePath());
        JasperReport jasperReport = JasperCompileManager.compileReport(new FileInputStream(templateFile));

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("createdBy", "Antigravity AI");

        JRDataSource jrDataSource;
        if (reportType.equals("tickets")) {
            jrDataSource = new JRBeanCollectionDataSource(ticketRepository.findAll());
        } else {
            // Load users via JDBC
            try (Connection connection = this.dataSource.getConnection()) {
                CustomUserRepository userRepository = new CustomUserRepository(connection);
                jrDataSource = new JRBeanCollectionDataSource(userRepository.searchUsers(""));
            }
        }

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, jrDataSource);

        if (format.equalsIgnoreCase("pdf")) {
            return JasperExportManager.exportReportToPdf(jasperPrint);
        } else if (format.equalsIgnoreCase("xlsx")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            JRXlsxExporter exporter = new JRXlsxExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
            exporter.exportReport();
            return out.toByteArray();
        }
        throw new IllegalArgumentException("Unsupported format: " + format);
    }

    @Async
    public void exportReportAsync(String jobId, String format, String reportType) {
        String templateFileName = reportType.equals("users") ? usersTemplate : ticketsTemplate;
        String createdBy = reportType.equals("users") ? "Admin" : "Ticket System";

        try {
            // Update status: PROCESSING
            jobService.updateProgress(jobId, 10);

            // 1. Load template từ external Jaspersoft workspace
            File templateFile = new File(templatePath, templateFileName);
            if (!templateFile.exists()) {
                throw new RuntimeException("Report template not found at: " + templateFile.getAbsolutePath());
            }
            System.out.println("Loading report template from: " + templateFile.getAbsolutePath());
            JasperReport jasperReport = JasperCompileManager.compileReport(new FileInputStream(templateFile));
            jobService.updateProgress(jobId, 20);

            // 2. Prepare parameters
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("createdBy", createdBy);

            // 3. Prepare data source
            JRDataSource jrDataSource;
            if (reportType.equals("users")) {
                try (Connection connection = this.dataSource.getConnection()) {
                    CustomUserRepository userRepository = new CustomUserRepository(connection);
                    jrDataSource = new JRBeanCollectionDataSource(userRepository.searchUsers(""));
                }
            } else {
                jrDataSource = new JRBeanCollectionDataSource(ticketRepository.findAll());
            }

            // 4. Fill report with data
            jobService.updateProgress(jobId, 30);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, jrDataSource);
            jobService.updateProgress(jobId, 60);

            // 5. Export to file
            String filePath = jobService.getFilePath(jobId, format);
            String fileName = reportType + "_report." + format;

            if (format.equalsIgnoreCase("pdf")) {
                JasperExportManager.exportReportToPdfFile(jasperPrint, filePath);
            } else if (format.equalsIgnoreCase("xlsx")) {
                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    JRXlsxExporter exporter = new JRXlsxExporter();
                    exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                    exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(fos));
                    exporter.exportReport();
                }
            }

            jobService.updateProgress(jobId, 90);

            // 6. Mark completed
            jobService.markCompleted(jobId, filePath, fileName);
            System.out.println("Report generated successfully: " + filePath);

        } catch (Exception e) {
            e.printStackTrace();
            jobService.markFailed(jobId, e.getMessage());
        }
    }
}
