package com.example.auth.service;

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.LoginResponse;
import com.example.auth.strategy.AuthenticationException;
import com.example.auth.strategy.AuthenticationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Authentication Service using Strategy Pattern
 * Supports: Database login, Federation login
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final List<AuthenticationStrategy> authenticationStrategies;

    public LoginResponse authenticate(LoginRequest request, String authType) throws AuthenticationException {
        AuthenticationStrategy strategy = findStrategy(authType);

        if (strategy == null) {
            throw new AuthenticationException(
                    "Unsupported authentication type: " + authType,
                    "UNSUPPORTED_AUTH_TYPE");
        }

        log.info("Using {} strategy for user {}", authType, request.getUsername());
        return strategy.authenticate(request);
    }

    /**
     * API đăng nhập sử dụng User Provider Database
     */
    public LoginResponse authenticateWithDatabase(LoginRequest request) throws AuthenticationException {
        return authenticate(request, "database");
    }

    /**
     * API đăng nhập sử dụng Remote User Federation
     */
    public LoginResponse authenticateWithFederation(LoginRequest request) throws AuthenticationException {
        return authenticate(request, "federation");
    }

    private AuthenticationStrategy findStrategy(String authType) {
        return authenticationStrategies.stream()
                .filter(strategy -> strategy.supports(authType))
                .findFirst()
                .orElse(null);
    }
}
