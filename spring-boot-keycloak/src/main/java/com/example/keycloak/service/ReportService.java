package com.example.keycloak.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    @Autowired
    private DataSource dataSource;

    public byte[] exportReport(String format) throws Exception {
        return exportReport(format, "/reports/users_report.jrxml", "Antigravity AI");
    }

    public byte[] exportTicketReport(String format) throws Exception {
        return exportReport(format, "/reports/tickets_report.jrxml", "Ticket System");
    }

    private byte[] exportReport(String format, String templatePath, String createdBy) throws Exception {
        // 1. Load file jrxml
        InputStream reportStream = getClass().getResourceAsStream(templatePath);
        if (reportStream == null) {
            throw new RuntimeException("Report template not found: " + templatePath);
        }
        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

        // 2. Map parameters (nếu có)
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("createdBy", createdBy);

        // 3. Fill report với dữ liệu từ Database
        try (Connection connection = dataSource.getConnection()) {
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, connection);

            // 4. Export theo định dạng yêu cầu
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
        throw new IllegalArgumentException("Unsupported format: " + format);
    }
}
