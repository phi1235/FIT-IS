package com.example.auth.service;

import com.example.auth.entity.AuthUser;
import com.example.auth.model.EmailAuditLog;
import com.example.auth.model.EmailAuditLog.EmailStatus;
import com.example.auth.model.EmailTemplate;
import com.example.auth.repository.EmailAuditLogRepository;
import com.example.auth.repository.EmailTemplateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for sending emails with dynamic templates
 */
@Slf4j
@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private TemplateEngine templateEngine;
    
    @Autowired
    private EmailTemplateRepository emailTemplateRepository;
    
    @Autowired
    private EmailAuditLogRepository emailAuditLogRepository;
    
    @Value("${spring.mail.from:noreply@banking.com}")
    private String fromEmail;
    
    @Value("${spring.mail.from-name:Banking System}")
    private String fromName;
    
    /**
     * Send password reset email
     */
    @Transactional
    public void sendPasswordResetEmail(AuthUser user, String resetCode) {
        log.info("Sending password reset email to: {}", user.getEmail());
        
        EmailTemplate template = emailTemplateRepository
                .findByTemplateCodeAndActiveTrue("PASSWORD_RESET")
                .orElseThrow(() -> new RuntimeException("Email template PASSWORD_RESET not found"));
        
        Map<String, String> variables = new HashMap<>();
        variables.put("userName", user.getFullName() != null ? user.getFullName() : user.getUsername());
        variables.put("resetCode", resetCode);
        variables.put("expiryMinutes", "15");
        variables.put("supportEmail", "support@banking.com");
        
        String htmlContent = processTemplate(template.getHtmlBody(), variables);
        String textContent = processTemplate(template.getTextBody(), variables);
        
        try {
            sendHtmlEmail(user.getEmail(), template.getSubject(), htmlContent, textContent);
            
            // Log email audit
            EmailAuditLog auditLog = EmailAuditLog.builder()
                    .user(user)
                    .templateCode("PASSWORD_RESET")
                    .recipientEmail(user.getEmail())
                    .status(EmailStatus.SENT)
                    .sentAt(LocalDateTime.now())
                    .build();
            emailAuditLogRepository.save(auditLog);
            
            log.info("Password reset email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset email", e);
            
            // Log email audit failure
            EmailAuditLog auditLog = EmailAuditLog.builder()
                    .user(user)
                    .templateCode("PASSWORD_RESET")
                    .recipientEmail(user.getEmail())
                    .status(EmailStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .sentAt(LocalDateTime.now())
                    .build();
            emailAuditLogRepository.save(auditLog);
            
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
    
    /**
     * Send password changed confirmation email
     */
    @Transactional
    public void sendPasswordChangedConfirmation(AuthUser user) {
        log.info("Sending password changed confirmation email to: {}", user.getEmail());
        
        EmailTemplate template = emailTemplateRepository
                .findByTemplateCodeAndActiveTrue("PASSWORD_CHANGED")
                .orElseThrow(() -> new RuntimeException("Email template PASSWORD_CHANGED not found"));
        
        Map<String, String> variables = new HashMap<>();
        variables.put("userName", user.getFullName() != null ? user.getFullName() : user.getUsername());
        variables.put("changedAt", LocalDateTime.now().toString());
        variables.put("supportEmail", "support@banking.com");
        
        String htmlContent = processTemplate(template.getHtmlBody(), variables);
        String textContent = processTemplate(template.getTextBody(), variables);
        
        try {
            sendHtmlEmail(user.getEmail(), template.getSubject(), htmlContent, textContent);
            
            // Log email audit
            EmailAuditLog auditLog = EmailAuditLog.builder()
                    .user(user)
                    .templateCode("PASSWORD_CHANGED")
                    .recipientEmail(user.getEmail())
                    .status(EmailStatus.SENT)
                    .sentAt(LocalDateTime.now())
                    .build();
            emailAuditLogRepository.save(auditLog);
            
            log.info("Password changed confirmation email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password changed confirmation email", e);
            
            // Log email audit failure
            EmailAuditLog auditLog = EmailAuditLog.builder()
                    .user(user)
                    .templateCode("PASSWORD_CHANGED")
                    .recipientEmail(user.getEmail())
                    .status(EmailStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .sentAt(LocalDateTime.now())
                    .build();
            emailAuditLogRepository.save(auditLog);
            
            throw new RuntimeException("Failed to send password changed confirmation email", e);
        }
    }
    
    /**
     * Send admin password reset email
     */
    @Transactional
    public void sendAdminResetEmail(AuthUser user, String resetCode) {
        log.info("Sending admin password reset email to: {}", user.getEmail());
        
        EmailTemplate template = emailTemplateRepository
                .findByTemplateCodeAndActiveTrue("ADMIN_PASSWORD_RESET")
                .orElseThrow(() -> new RuntimeException("Email template ADMIN_PASSWORD_RESET not found"));
        
        Map<String, String> variables = new HashMap<>();
        variables.put("userName", user.getFullName() != null ? user.getFullName() : user.getUsername());
        variables.put("resetCode", resetCode);
        variables.put("expiryMinutes", "60");
        variables.put("supportEmail", "support@banking.com");
        
        String htmlContent = processTemplate(template.getHtmlBody(), variables);
        String textContent = processTemplate(template.getTextBody(), variables);
        
        try {
            sendHtmlEmail(user.getEmail(), template.getSubject(), htmlContent, textContent);
            
            // Log email audit
            EmailAuditLog auditLog = EmailAuditLog.builder()
                    .user(user)
                    .templateCode("ADMIN_PASSWORD_RESET")
                    .recipientEmail(user.getEmail())
                    .status(EmailStatus.SENT)
                    .sentAt(LocalDateTime.now())
                    .build();
            emailAuditLogRepository.save(auditLog);
            
            log.info("Admin password reset email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send admin password reset email", e);
            
            // Log email audit failure
            EmailAuditLog auditLog = EmailAuditLog.builder()
                    .user(user)
                    .templateCode("ADMIN_PASSWORD_RESET")
                    .recipientEmail(user.getEmail())
                    .status(EmailStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .sentAt(LocalDateTime.now())
                    .build();
            emailAuditLogRepository.save(auditLog);
            
            throw new RuntimeException("Failed to send admin password reset email", e);
        }
    }
    
    /**
     * Send HTML email with both HTML and text alternatives
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent, String textContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textContent, htmlContent);
            
            mailSender.send(message);
            log.debug("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Error sending email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    /**
     * Send simple text email
     */
    private void sendSimpleEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            
            mailSender.send(message);
            log.debug("Simple email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Error sending simple email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    /**
     * Process template with variable substitution using simple string replacement
     * Template variables should be in the format {{variableName}}
     */
    private String processTemplate(String template, Map<String, String> variables) {
        if (template == null) {
            return "";
        }
        
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }
        
        return result;
    }
}
