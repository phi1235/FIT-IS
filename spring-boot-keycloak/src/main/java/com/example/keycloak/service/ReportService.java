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

    // Template cache để tránh compile lại mỗi lần (tối ưu performance)
    private final Map<String, JasperReport> templateCache = new ConcurrentHashMap<>();

    // Track file modification time để invalidate cache khi template thay đổi
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
     * Load và cache compiled template (tối ưu: chỉ compile 1 lần, reuse sau đó)
     */
    private JasperReport getOrCompileTemplate(String templateFileName) throws Exception {
        File templateFile = new File(templatePath, templateFileName);
        if (!templateFile.exists()) {
            throw new RuntimeException("Report template not found at: " + templateFile.getAbsolutePath());
        }

        long lastModified = templateFile.lastModified();
        String cacheKey = templateFileName;

        // Kiểm tra cache: nếu template đã compile và file chưa thay đổi thì dùng cache
        if (templateCache.containsKey(cacheKey)) {
            Long cachedModified = templateLastModified.get(cacheKey);
            if (cachedModified != null && cachedModified == lastModified) {
                System.out.println("Using cached template: " + templateFileName);
                return templateCache.get(cacheKey);
            } else {
                // Template đã thay đổi, invalidate cache
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
            // Log chi tiết để debug
            System.err.println("=== JASPER REPORT COMPILE ERROR ===");
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

    /**
     * Custom DataSource để stream data từ ResultSet
     * Thay vì load tất cả 999,997 users vào memory (gây hang/OutOfMemory)
     * DataSource này sẽ stream từng row khi JasperReports cần
     */
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
            // ResultSet không support moveFirst, cần re-execute query
            // JasperReports có thể gọi method này, nhưng với streaming data source thì
            // không cần
            throw new UnsupportedOperationException("ResultSet does not support moveFirst - use streaming mode");
        }
    }

    /**
     * Tối ưu: Dùng custom StreamingResultSetDataSource để stream data trực tiếp từ
     * database
     * Thay vì load tất cả 999,997 users vào memory (gây hang/OutOfMemory)
     * DataSource này sẽ stream từng row khi JasperReports cần
     * 
     * Note: Connection, PreparedStatement và ResultSet sẽ được quản lý bởi caller
     * (trong try-with-resources block) để đảm bảo chúng không bị đóng sớm
     */
    private JRDataSource createUsersDataSource(Connection connection) throws Exception {
        // #region agent log
        try {
            java.io.FileWriter fw = new java.io.FileWriter(
                    "/home/nguyen-phi/Downloads/angular project/.cursor/debug.log", true);
            fw.write(
                    "{\"location\":\"ReportService.java:101\",\"message\":\"createUsersDataSource started - using streaming\",\"data\":{},\"timestamp\":"
                            + System.currentTimeMillis()
                            + ",\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\"}\n");
            fw.close();
        } catch (Exception e) {
        }
        // #endregion

        // Dùng SQL query trực tiếp với ResultSet streaming
        // JasperReports sẽ đọc từng row khi cần, không load tất cả vào memory
        String sql = "SELECT id, username, email, first_name, last_name, enabled, role FROM users ORDER BY username";
        java.sql.PreparedStatement pstmt = connection.prepareStatement(sql,
                java.sql.ResultSet.TYPE_FORWARD_ONLY,
                java.sql.ResultSet.CONCUR_READ_ONLY);

        // Set fetch size để stream data (không load tất cả cùng lúc)
        // TỐI ƯU: Tăng fetch size lên 2000 để giảm số lần round-trip đến database
        pstmt.setFetchSize(2000); // Load 2000 rows mỗi lần từ database

        java.sql.ResultSet rs = pstmt.executeQuery();

        // #region agent log
        try {
            java.io.FileWriter fw = new java.io.FileWriter(
                    "/home/nguyen-phi/Downloads/angular project/.cursor/debug.log", true);
            fw.write(
                    "{\"location\":\"ReportService.java:115\",\"message\":\"ResultSet created for streaming\",\"data\":{},\"timestamp\":"
                            + System.currentTimeMillis()
                            + ",\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\"}\n");
            fw.close();
        } catch (Exception e) {
        }
        // #endregion

        // Custom StreamingResultSetDataSource sẽ stream data từ ResultSet, không load
        // tất cả vào memory
        // Connection và PreparedStatement sẽ được giữ mở cho đến khi fillReport() hoàn
        // thành
        // (được quản lý bởi try-with-resources trong exportReportAsync)
        return new StreamingResultSetDataSource(rs);
    }

    private byte[] exportReportFromExternal(String format, String templateFileName, String reportType)
            throws Exception {
        // Load template (cached nếu có)
        JasperReport jasperReport = getOrCompileTemplate(templateFileName);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("createdBy", "Antigravity AI");

        JRDataSource jrDataSource;
        if (reportType.equals("tickets")) {
            // Tickets thường ít hơn, có thể load vào memory
            jrDataSource = new JRBeanCollectionDataSource(ticketRepository.findAll());
        } else {
            // Users: dùng JDBC streaming để tối ưu memory
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

        // Fallback cho tickets
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
        // #region agent log
        try {
            java.io.FileWriter fw = new java.io.FileWriter(
                    "/home/nguyen-phi/Downloads/angular project/.cursor/debug.log", true);
            fw.write(
                    "{\"location\":\"ReportService.java:155\",\"message\":\"exportReportAsync started\",\"data\":{\"jobId\":\""
                            + jobId + "\",\"format\":\"" + format + "\",\"reportType\":\"" + reportType
                            + "\"},\"timestamp\":" + System.currentTimeMillis()
                            + ",\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"D\"}\n");
            fw.close();
        } catch (Exception e) {
        }
        // #endregion

        String templateFileName = reportType.equals("users") ? usersTemplate : ticketsTemplate;
        String createdBy = reportType.equals("users") ? "Admin" : "Ticket System";

        try {
            // Update status: PROCESSING
            jobService.updateProgress(jobId, 10);

            // 1. Load template (cached nếu có) - TỐI ƯU: không compile lại mỗi lần
            JasperReport jasperReport = getOrCompileTemplate(templateFileName);
            jobService.updateProgress(jobId, 20);

            // 2. Prepare parameters
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("createdBy", createdBy);

            // 3. Prepare data source - TỐI ƯU: dùng JDBC streaming cho users
            JRDataSource jrDataSource;
            if (reportType.equals("users")) {
                // TỐI ƯU: Dùng JRFileVirtualizer để swap pages ra disk
                // Giữ tối đa 100 pages trong memory, còn lại swap ra disk
                JRFileVirtualizer virtualizer = new JRFileVirtualizer(100, System.getProperty("java.io.tmpdir"));
                parameters.put(JRParameter.REPORT_VIRTUALIZER, virtualizer);

                try (Connection connection = this.dataSource.getConnection()) {
                    // Tối ưu: dùng JDBC ResultSet streaming thay vì load tất cả vào memory
                    jrDataSource = createUsersDataSource(connection);

                    // 4. Fill report with data
                    jobService.updateProgress(jobId, 30);
                    System.out.println("Filling report with virtualizer enabled...");
                    JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, jrDataSource);
                    jobService.updateProgress(jobId, 60);
                    System.out.println("Report filled, exporting to file...");
                    // #region agent log
                    try {
                        java.io.FileWriter fw = new java.io.FileWriter(
                                "/home/nguyen-phi/Downloads/angular project/.cursor/debug.log", true);
                        fw.write(
                                "{\"location\":\"ReportService.java:181\",\"message\":\"report filled, exporting to file\",\"data\":{\"jobId\":\""
                                        + jobId + "\",\"progress\":60},\"timestamp\":" + System.currentTimeMillis()
                                        + ",\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"B\"}\n");
                        fw.close();
                    } catch (Exception e) {
                    }
                    // #endregion

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
                    jobService.markCompleted(jobId, filePath, fileName);
                    System.out.println("Report generated successfully: " + filePath);
                    // #region agent log
                    try {
                        java.io.FileWriter fw = new java.io.FileWriter(
                                "/home/nguyen-phi/Downloads/angular project/.cursor/debug.log", true);
                        fw.write(
                                "{\"location\":\"ReportService.java:200\",\"message\":\"report completed\",\"data\":{\"jobId\":\""
                                        + jobId + "\",\"filePath\":\"" + filePath + "\"},\"timestamp\":"
                                        + System.currentTimeMillis()
                                        + ",\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"D\"}\n");
                        fw.close();
                    } catch (Exception e) {
                    }
                    // #endregion
                    return; // Exit early for users
                }
            } else {
                // Tickets: load vào memory (thường ít hơn)
                jrDataSource = new JRBeanCollectionDataSource(ticketRepository.findAll());
            }

            // 4. Fill report with data (for tickets)
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
            // #region agent log
            try {
                java.io.FileWriter fw = new java.io.FileWriter(
                        "/home/nguyen-phi/Downloads/angular project/.cursor/debug.log", true);
                fw.write(
                        "{\"location\":\"ReportService.java:234\",\"message\":\"report generation failed\",\"data\":{\"jobId\":\""
                                + jobId + "\",\"error\":\"" + e.getMessage().replace("\"", "\\\"")
                                + "\",\"errorClass\":\"" + e.getClass().getName() + "\"},\"timestamp\":"
                                + System.currentTimeMillis()
                                + ",\"sessionId\":\"debug-session\",\"runId\":\"run1\",\"hypothesisId\":\"D\"}\n");
                fw.close();
            } catch (Exception logEx) {
            }
            // #endregion
            jobService.markFailed(jobId, e.getMessage());
        }
    }
}
