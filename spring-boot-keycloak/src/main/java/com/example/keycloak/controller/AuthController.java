package com.example.keycloak.controller;

import com.example.keycloak.dto.ChangePasswordRequest;
import com.example.keycloak.dto.LoginRequest;
import com.example.keycloak.dto.LoginResponse;
import com.example.keycloak.dto.RegisterRequest;
import com.example.keycloak.dto.UserDTO;
import com.example.keycloak.service.AuthenticationService;
import com.example.keycloak.service.RsaService;
import com.example.keycloak.service.UserService;
import com.example.keycloak.strategy.AuthenticationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthenticationService authenticationService;
    private final RsaService rsaService;
    private final UserService userService;

    /**
     * Get RSA public key for password encryption
     */
    @GetMapping("/public-key")
    public ResponseEntity<?> getPublicKey() {
        return ResponseEntity.ok(Map.of(
                "publicKey", rsaService.getPublicKeyPem(),
                "algorithm", "RSA-OAEP",
                "keySize", 2048));
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
     * API đăng nhập generic - cho phép chọn authentication type
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            @RequestParam(defaultValue = "database") String authType) throws AuthenticationException {
        LoginResponse response = authenticationService.authenticate(request, authType);
        return ResponseEntity.ok(response);
    }

    /**
     * Đăng ký user mới - Public endpoint
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            UserDTO user = userService.registerUser(request);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "User registered successfully",
                    "user", user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()));
        }
    }

    @PostMapping("/password/change")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        try {
            String username = request.getUsername();

            boolean success = userService.changePassword(username, request);
            if (success) {
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Password changed successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Failed to change password"));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()));
        }
    }

    /**
     * Public endpoint - không cần authentication
     */
    @GetMapping("/public/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Service is running");
    }
}

