package com.example.auth.controller;

import com.example.auth.dto.EmailTemplateDTO;
import com.example.auth.model.EmailTemplate;
import com.example.auth.repository.EmailTemplateRepository;
import com.example.auth.entity.AuthUser;
import com.example.auth.repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/auth/admin/email-templates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EmailTemplateController {

    private final EmailTemplateRepository emailTemplateRepository;
    private final AuthUserRepository authUserRepository;

    @GetMapping
    public ResponseEntity<Page<EmailTemplateDTO>> getAllTemplates(Pageable pageable) {
        return ResponseEntity.ok(emailTemplateRepository.findAll(pageable)
                .map(this::convertToDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailTemplateDTO> getTemplateById(@PathVariable UUID id) {
        return emailTemplateRepository.findById(id)
                .map(this::convertToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<EmailTemplateDTO> createTemplate(
            @Valid @RequestBody EmailTemplateDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        AuthUser currentUser = authUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        EmailTemplate template = EmailTemplate.builder()
                .templateCode(dto.getTemplateCode())
                .templateName(dto.getTemplateName())
                .subject(dto.getSubject())
                .htmlBody(dto.getHtmlBody())
                .textBody(dto.getTextBody())
                .requiredVariables(dto.getRequiredVariables())
                .active(true)
                .version(1)
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        return ResponseEntity.ok(convertToDTO(emailTemplateRepository.save(template)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmailTemplateDTO> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody EmailTemplateDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        EmailTemplate template = emailTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        
        AuthUser currentUser = authUserRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        template.setTemplateName(dto.getTemplateName());
        template.setSubject(dto.getSubject());
        template.setHtmlBody(dto.getHtmlBody());
        template.setTextBody(dto.getTextBody());
        template.setRequiredVariables(dto.getRequiredVariables());
        template.setActive(dto.isActive());
        template.setVersion(template.getVersion() + 1);
        template.setUpdatedBy(currentUser);

        return ResponseEntity.ok(convertToDTO(emailTemplateRepository.save(template)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        EmailTemplate template = emailTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        
        template.setActive(false);
        emailTemplateRepository.save(template);
        return ResponseEntity.noContent().build();
    }

    private EmailTemplateDTO convertToDTO(EmailTemplate template) {
        return EmailTemplateDTO.builder()
                .id(template.getId())
                .templateCode(template.getTemplateCode())
                .templateName(template.getTemplateName())
                .subject(template.getSubject())
                .htmlBody(template.getHtmlBody())
                .textBody(template.getTextBody())
                .requiredVariables(template.getRequiredVariables())
                .version(template.getVersion())
                .active(template.isActive())
                .createdBy(template.getCreatedBy() != null ? template.getCreatedBy().getUsername() : "system")
                .updatedBy(template.getUpdatedBy() != null ? template.getUpdatedBy().getUsername() : "system")
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
