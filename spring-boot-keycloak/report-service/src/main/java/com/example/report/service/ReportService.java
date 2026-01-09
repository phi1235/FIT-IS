package com.example.report.service;

import com.example.report.model.ReportJob;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Report Service - handles JasperReports generation
 */
@Slf4j
@Service
public class ReportService {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ReportJobService jobService;

    @Value("${jasper.template-path:/app/jasper-templates}")
    private String templatePath;

    @Value("${jasper.output-path:/tmp/reports}")
    private String outputPath;

    private final Map<String, JasperReport> compiledTemplates = new ConcurrentHashMap<>();

    /**
     * Async report generation
     */
    @Async
    public void exportReportAsync(String jobId, String format, String reportType) {
        try {
            jobService.updateProgress(jobId, 10);

            String templateFileName = reportType.equals("users") ? "Invoice.jrxml" : "tickets_report.jrxml";
            
            jobService.updateProgress(jobId, 30);

            JasperReport jasperReport = getOrCompileTemplate(templateFileName);
            
            jobService.updateProgress(jobId, 50);

            JasperPrint jasperPrint;
            try (Connection connection = dataSource.getConnection()) {
                jasperPrint = JasperFillManager.fillReport(jasperReport, new HashMap<>(), connection);
            }
            
            jobService.updateProgress(jobId, 70);

            // Create output directory if not exists
            File outputDir = new File(outputPath);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            String fileName = reportType + "_report_" + jobId + "." + format;
            String filePath = outputPath + "/" + fileName;

            exportToFile(format, jasperPrint, filePath);
            
            jobService.markCompleted(jobId, fileName, filePath);

        } catch (Exception e) {
            log.error("Report generation failed for job {}: {}", jobId, e.getMessage(), e);
            jobService.markFailed(jobId, e.getMessage());
        }
    }

    private JasperReport getOrCompileTemplate(String templateFileName) throws JRException, FileNotFoundException {
        if (compiledTemplates.containsKey(templateFileName)) {
            return compiledTemplates.get(templateFileName);
        }

        String templateFullPath = templatePath + "/" + templateFileName;
        File templateFile = new File(templateFullPath);

        if (!templateFile.exists()) {
            throw new FileNotFoundException("Template not found: " + templateFullPath);
        }

        try (InputStream is = new FileInputStream(templateFile)) {
            JasperReport report = JasperCompileManager.compileReport(is);
            compiledTemplates.put(templateFileName, report);
            log.info("Compiled template: {}", templateFileName);
            return report;
        } catch (IOException e) {
            throw new JRException("Failed to read template file", e);
        }
    }

    private void exportToFile(String format, JasperPrint jasperPrint, String filePath) throws JRException {
        if (format.equalsIgnoreCase("pdf")) {
            JasperExportManager.exportReportToPdfFile(jasperPrint, filePath);
        } else if (format.equalsIgnoreCase("xlsx")) {
            JRXlsxExporter exporter = new JRXlsxExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(filePath));
            exporter.exportReport();
        }
        log.info("Report exported to: {}", filePath);
    }
}
