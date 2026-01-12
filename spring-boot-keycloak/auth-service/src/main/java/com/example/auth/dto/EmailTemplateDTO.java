package com.example.auth.dto;

import lombok.*;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplateDTO {
    private UUID id;
    
    @NotBlank(message = "Template code is required")
    private String templateCode;
    
    @NotBlank(message = "Template name is required")
    private String templateName;
    
    @NotBlank(message = "Subject is required")
    private String subject;
    
    @NotBlank(message = "HTML body is required")
    private String htmlBody;
    
    private String textBody;
    
    private List<String> requiredVariables;
    
    private int version;
    
    private boolean active;
    
    private String createdBy;
    
    private String updatedBy;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
