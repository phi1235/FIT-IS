package com.example.keycloak.service;

import com.example.keycloak.dto.LoginRequest;
import com.example.keycloak.dto.LoginResponse;
import com.example.keycloak.strategy.AuthenticationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AuthService - Facade cho AuthenticationService
 * Giữ lại interface cũ để backward compatibility
 * Sử dụng AuthenticationService với Strategy Pattern bên trong
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final AuthenticationService authenticationService;
    
    /**
     * Đăng nhập sử dụng User Provider Database (Keycloak internal database)
     * @deprecated Sử dụng AuthenticationService.authenticateWithDatabase() thay thế
     */
    @Deprecated
    public LoginResponse loginWithDatabase(LoginRequest request) {
        try {
            return authenticationService.authenticateWithDatabase(request);
        } catch (AuthenticationException e) {
            log.error("Database login failed: {}", e.getMessage());
            throw new RuntimeException("Invalid credentials", e);
        }
    }
    
    /**
     * Đăng nhập sử dụng Remote User Federation
     * @deprecated Sử dụng AuthenticationService.authenticateWithFederation() thay thế
     */
    @Deprecated
    public LoginResponse loginWithFederation(LoginRequest request) {
        try {
            return authenticationService.authenticateWithFederation(request);
        } catch (AuthenticationException e) {
            log.error("Federation login failed: {}", e.getMessage());
            throw new RuntimeException("Federation authentication failed", e);
        }
    }
}

