package com.example.keycloak.service;

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
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.jasperreports.engine.fill.JRFileVirtualizer;

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

    private final Map<String, JasperReport> templateCache = new ConcurrentHashMap<>();
    private final Map<String, Long> templateLastModified = new ConcurrentHashMap<>();

    /**
     * Load template từ Jaspersoft workspace bên ngoài
     */
    public byte[] exportReport(String format) throws Exception {
        return exportReportFromExternal(format, usersTemplate, "users");
    }

    public byte[] exportTicketReport(String format) throws Exception {
        return exportReportFromExternal(format, ticketsTemplate, "tickets");
    }

    /**
     * Load và cache compiled template ( chỉ compile 1 lần, reuse sau đó)
     */
    private JasperReport getOrCompileTemplate(String templateFileName) throws Exception {
        File templateFile = new File(templatePath, templateFileName);
        if (!templateFile.exists()) {
            throw new RuntimeException("Report template not found at: " + templateFile.getAbsolutePath());
        }

        long lastModified = templateFile.lastModified();
        String cacheKey = templateFileName;

        // nếu template đã compile và file chưa thay đổi thì dùng cache
        if (templateCache.containsKey(cacheKey)) {
            Long cachedModified = templateLastModified.get(cacheKey);
            if (cachedModified != null && cachedModified == lastModified) {
                System.out.println("Using cached template: " + templateFileName);
                return templateCache.get(cacheKey);
            } else {
                templateCache.remove(cacheKey);
                templateLastModified.remove(cacheKey);
            }
        }

        // Compile template mới và cache lại
        System.out.println("Compiling template: " + templateFile.getAbsolutePath());
        try {
            JasperReport jasperReport = JasperCompileManager.compileReport(new FileInputStream(templateFile));
            templateCache.put(cacheKey, jasperReport);
            templateLastModified.put(cacheKey, lastModified);
            return jasperReport;
        } catch (Exception e) {
            // Log debug
            System.err.println(" JASPER REPORT COMPILE ERROR");
            System.err.println("Template file: " + templateFile.getAbsolutePath());
            System.err.println("Error message: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Cause: " + e.getCause().getMessage());
                if (e.getCause().getCause() != null) {
                    System.err.println("Root cause: " + e.getCause().getCause().getMessage());
                }
            }
            e.printStackTrace();
            throw e;
        }
    }

    private static class StreamingResultSetDataSource implements JRDataSource {
        private final java.sql.ResultSet resultSet;

        public StreamingResultSetDataSource(java.sql.ResultSet resultSet) {
            this.resultSet = resultSet;
        }

        @Override
        public boolean next() throws JRException {
            try {
                return resultSet.next();
            } catch (java.sql.SQLException e) {
                throw new JRException("Error reading next row", e);
            }
        }

        @Override
        public Object getFieldValue(JRField field) throws JRException {
            try {
                String fieldName = field.getName();
                // Map field names từ JasperReports template sang database columns
                switch (fieldName) {
                    case "id":
                        return resultSet.getString("id");
                    case "username":
                        return resultSet.getString("username");
                    case "email":
                        return resultSet.getString("email");
                    case "firstName":
                    case "first_name":
                        return resultSet.getString("first_name");
                    case "lastName":
                    case "last_name":
                        return resultSet.getString("last_name");
                    case "enabled":
                        return resultSet.getBoolean("enabled");
                    case "role":
                        return resultSet.getString("role");
                    default:
                        return null;
                }
            } catch (java.sql.SQLException e) {
                throw new JRException("Error reading field: " + field.getName(), e);
            }
        }

        public void moveFirst() throws JRException {
            // JasperReports có thể gọi method(streaming data source thì không cần) 
            throw new UnsupportedOperationException("ResultSet does not support moveFirst - use streaming mode");
        }
    }

    private JRDataSource createUsersDataSource(Connection connection) throws Exception {        // Dùng SQL query trực tiếp với ResultSet streaming
        // JasperReports sẽ đọc từng row khi cần, không load tất cả vào memory
        String sql = "SELECT id, username, email, first_name, last_name, enabled, role FROM users ORDER BY username";
        java.sql.PreparedStatement pstmt = connection.prepareStatement(sql,
                java.sql.ResultSet.TYPE_FORWARD_ONLY,
                java.sql.ResultSet.CONCUR_READ_ONLY);

        pstmt.setFetchSize(2000); 
        // Load 2000 rows mỗi lần

        java.sql.ResultSet rs = pstmt.executeQuery();
        return new StreamingResultSetDataSource(rs);
    }
    /**
     * map dữ liệu từ repository sang DTO
     */
    private byte[] exportReportFromExternal(String format, String templateFileName, String reportType)
            throws Exception {
        // Load template cached nếu có
        JasperReport jasperReport = getOrCompileTemplate(templateFileName);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("createdBy", "Admin");

        JRDataSource jrDataSource;
        if (reportType.equals("tickets")) {
            java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("d-M-yyyy");
            java.util.List<com.example.keycloak.dto.TicketReportDTO> ticketDTOs = ticketRepository.findAll().stream()
                    .map(t -> com.example.keycloak.dto.TicketReportDTO.builder()
                            .id(t.getId())
                            .title(t.getTitle())
                            .status(t.getStatus() != null ? t.getStatus().name() : null)
                            .amount(t.getAmount())
                            .maker(t.getMaker())
                            .checker(t.getChecker())
                            .createdAt(t.getCreatedAt() != null ? t.getCreatedAt().format(dateFormatter) : null)
                            .build())
                    .collect(java.util.stream.Collectors.toList());
            // Tạo DataSource từ danh sách DTO
            jrDataSource = new JRBeanCollectionDataSource(ticketDTOs);
        } else {
            // dùng JDBC streaming để tối ưu memory
            try (Connection connection = this.dataSource.getConnection()) {
                jrDataSource = createUsersDataSource(connection);
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
            }
        }

        // Đổ dữ liệu vào template để tạo file JasperPrint
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
    /**
     * Tạo job xuất report chạy nền
     */
    @Async
    public void exportReportAsync(String jobId, String format, String reportType) {
        String templateFileName = reportType.equals("users") ? usersTemplate : ticketsTemplate;
        String createdBy = reportType.equals("users") ? "Admin" : "Ticket System";

        try {
            jobService.updateProgress(jobId, 10);

            // Load template (không compile lại mỗi lần)
            JasperReport jasperReport = getOrCompileTemplate(templateFileName);
            jobService.updateProgress(jobId, 20);

            // Prepare parameters
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("createdBy", createdBy);

            JRDataSource jrDataSource;
            if (reportType.equals("users")) {
                // swap pages ra disk
                JRFileVirtualizer virtualizer = new JRFileVirtualizer(100, System.getProperty("java.io.tmpdir"));
                parameters.put(JRParameter.REPORT_VIRTUALIZER, virtualizer);

                try (Connection connection = this.dataSource.getConnection()) {
                    jrDataSource = createUsersDataSource(connection);

                    // Fill report with data
                    jobService.updateProgress(jobId, 30);
                    System.out.println("Filling report with virtualizer enabled...");
                    JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, jrDataSource);
                    jobService.updateProgress(jobId, 60);
                    System.out.println("Report filled, exporting to file...");                    // Export to file
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
                    jobService.markCompleted(jobId, filePath, fileName);
                    System.out.println("Report generated successfully: " + filePath);                  
                    return; // Exit early for users
                }
            } else {
                java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter
                        .ofPattern("d-M-yyyy");
                java.util.List<com.example.keycloak.dto.TicketReportDTO> ticketDTOs = ticketRepository.findAll()
                        .stream()
                        .map(t -> com.example.keycloak.dto.TicketReportDTO.builder()
                                .id(t.getId())
                                .title(t.getTitle())
                                .status(t.getStatus() != null ? t.getStatus().name() : null)
                                .amount(t.getAmount())
                                .maker(t.getMaker())
                                .checker(t.getChecker())
                                .createdAt(t.getCreatedAt() != null ? t.getCreatedAt().format(dateFormatter) : null)
                                .build())
                        .collect(java.util.stream.Collectors.toList());
                jrDataSource = new JRBeanCollectionDataSource(ticketDTOs);
            }

            // Fill report with data
            jobService.updateProgress(jobId, 30);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, jrDataSource);
            jobService.updateProgress(jobId, 60);

            // Export to file
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

            // Mark completed
            jobService.markCompleted(jobId, filePath, fileName);
            System.out.println("Report generated successfully: " + filePath);

        } catch (Exception e) {
            e.printStackTrace();            
            jobService.markFailed(jobId, e.getMessage());
        }
    }
}
