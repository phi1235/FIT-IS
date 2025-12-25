package com.example.keycloak.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

/**
 * Request Size Limiting Configuration
 * 
 * - Giới hạn kích thước request body: 1MB (config trong application.yml)
 * - Giới hạn file upload: 5MB (config trong application.yml)
 * - Giới hạn số lượng parameters: 1000
 * 
 * Note: Request size limits được config trong application.yml:
 * - server.max-http-post-size: 1MB
 * - spring.servlet.multipart.max-file-size: 5MB
 * - spring.servlet.multipart.max-request-size: 5MB
 */
@Configuration
public class RequestSizeConfig {
    
    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }
}
