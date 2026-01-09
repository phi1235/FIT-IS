package com.example.auth.repository;

import com.example.auth.model.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {
    
    Optional<EmailTemplate> findByTemplateCodeAndActiveTrue(String templateCode);
    
    Optional<EmailTemplate> findByTemplateCode(String templateCode);
}
