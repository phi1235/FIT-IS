package com.example.auth.strategy;

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.LoginResponse;
import com.example.auth.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Remote Federation Authentication Strategy
 * API đăng nhập sử dụng Remote User Federation
 * Validates credentials against a remote API endpoint
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FederationAuthenticationStrategy implements AuthenticationStrategy {

    private final JwtService jwtService;
    private final RestTemplate restTemplate;

    @Value("${federation.remote-api-url:http://localhost:9000/api/validate}")
    private String remoteApiUrl;

    @Override
    public LoginResponse authenticate(LoginRequest request) {
        log.info("Authenticating user {} with FEDERATION strategy", request.getUsername());

        try {
            // Call remote API to validate credentials
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, String> body = Map.of(
                    "username", request.getUsername(),
                    "password", request.getPassword()
            );

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    remoteApiUrl, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new AuthenticationException("Remote validation failed", "FEDERATION_FAILED");
            }

            Map<String, Object> remoteUser = response.getBody();
            String userId = String.valueOf(remoteUser.getOrDefault("userId", "remote-" + request.getUsername()));
            String email = String.valueOf(remoteUser.getOrDefault("email", request.getUsername() + "@remote.local"));
            Set<String> roleCodes = new HashSet<>(Arrays.asList("user")); // Default role for federation
            
            String accessToken = jwtService.generateToken(request.getUsername(), roleCodes, 
                    "federated-user", request.getUsername());
            String refreshToken = jwtService.generateRefreshToken(request.getUsername());

            log.info("Federation authentication successful for user: {}", request.getUsername());

            return LoginResponse.builder()
                    .user(LoginResponse.UserInfo.builder()
                            .username(request.getUsername())
                            .roles(roleCodes)
                            .build())
                    .token(LoginResponse.TokenInfo.builder()
                            .accessToken(accessToken)
                            .refreshToken(refreshToken)
                            .tokenType("Bearer")
                            .expiresIn(jwtService.getExpirationTimeInSeconds())
                            .build())
                    .build();

        } catch (Exception e) {
            log.error("Federation authentication failed: {}", e.getMessage());
            throw new AuthenticationException("Federation authentication failed: " + e.getMessage(), "FEDERATION_ERROR");
        }
    }

    @Override
    public boolean supports(String authType) {
        return "federation".equalsIgnoreCase(authType);
    }
}
