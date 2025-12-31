package com.example.keycloak.service;

import com.example.keycloak.dto.LoginRequest;
import com.example.keycloak.dto.LoginResponse;
import com.example.keycloak.strategy.AuthenticationException;
import com.example.keycloak.strategy.AuthenticationStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service quản lý authentication sử dụng Strategy Pattern
 * Context class trong Strategy Pattern
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

    public LoginResponse authenticateWithDatabase(LoginRequest request) throws AuthenticationException {
        return authenticate(request, "database");
    }

    /**
     * Authenticate với federation strategy
     */
    public LoginResponse authenticateWithFederation(LoginRequest request) throws AuthenticationException {
        return authenticate(request, "federation");
    }

    /**
     * Tìm strategy phù hợp với authType
     */
    private AuthenticationStrategy findStrategy(String authType) {
        return authenticationStrategies.stream()
                .filter(strategy -> strategy.supports(authType))
                .findFirst()
                .orElse(null);
    }
}
