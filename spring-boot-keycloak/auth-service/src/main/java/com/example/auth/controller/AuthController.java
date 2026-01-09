package com.example.auth.controller;

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.LoginResponse;
import com.example.auth.service.AuthenticationService;
import com.example.auth.strategy.AuthenticationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

/**
 * Authentication Controller
 * - API đăng nhập sử dụng User Provider Database
 * - API đăng nhập sử dụng Remote User Federation
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthenticationService authenticationService;
    private final com.example.auth.service.RsaService rsaService;

    /**
     * Get RSA public key for password encryption
     */
    @GetMapping("/public-key")
    public ResponseEntity<Map<String, String>> getPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", rsaService.getPublicKeyPem()));
    }

    /**
     * API đăng nhập sử dụng User Provider Database
     */
    @PostMapping("/login/database")
    public ResponseEntity<LoginResponse> loginWithDatabase(
            @Valid @RequestBody LoginRequest request) throws AuthenticationException {
        LoginResponse response = authenticationService.authenticateWithDatabase(request);
        return ResponseEntity.ok(response);
    }

    /**
     * API đăng nhập sử dụng Remote User Federation
     */
    @PostMapping("/login/federation")
    public ResponseEntity<LoginResponse> loginWithFederation(
            @Valid @RequestBody LoginRequest request) throws AuthenticationException {
        LoginResponse response = authenticationService.authenticateWithFederation(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Generic login endpoint - cho phép chọn authentication type
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            @RequestParam(defaultValue = "database") String authType) throws AuthenticationException {
        LoginResponse response = authenticationService.authenticate(request, authType);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/public/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth Service is running");
    }
}
