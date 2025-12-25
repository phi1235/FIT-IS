package com.example.keycloak.controller;

import com.example.keycloak.dto.ChangePasswordRequest;
import com.example.keycloak.dto.LoginRequest;
import com.example.keycloak.dto.LoginResponse;
import com.example.keycloak.dto.MfaRequest;
import com.example.keycloak.dto.MfaSetupResponse;
import com.example.keycloak.dto.RegisterRequest;
import com.example.keycloak.dto.UserDTO;
import com.example.keycloak.service.AuthenticationService;
import com.example.keycloak.service.MfaService;
import com.example.keycloak.service.UserService;
import com.example.keycloak.strategy.AuthenticationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final MfaService mfaService;
    private final UserService userService;
    
    /**
     * API đăng nhập sử dụng User Provider Database
     * Sử dụng Strategy Pattern - DatabaseAuthenticationStrategy
     */
    @PostMapping("/login/database")
    public ResponseEntity<LoginResponse> loginWithDatabase(
            @Valid @RequestBody LoginRequest request) throws AuthenticationException {
        // Username đã được trim bởi TrimStringDeserializer trước khi validation
        LoginResponse response = authenticationService.authenticateWithDatabase(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * API đăng nhập sử dụng Remote User Federation
     * Sử dụng Strategy Pattern - FederationAuthenticationStrategy
     */
    @PostMapping("/login/federation")
    public ResponseEntity<LoginResponse> loginWithFederation(
            @Valid @RequestBody LoginRequest request) throws AuthenticationException {
        // Username đã được trim bởi TrimStringDeserializer trước khi validation
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
        // Username đã được trim bởi TrimStringDeserializer trước khi validation
        LoginResponse response = authenticationService.authenticate(request, authType);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Setup MFA cho user - tạo secret và QR code
     */
    @PostMapping("/mfa/setup")
    public ResponseEntity<MfaSetupResponse> setupMfa(@RequestParam String username) {
        String secret = mfaService.generateSecret(username);
        String qrCodeUrl = mfaService.generateQrCodeUrl(username, secret, "Bank App");
        String manualEntryKey = mfaService.getSecret(username);
        
        MfaSetupResponse response = MfaSetupResponse.builder()
            .secret(secret)
            .qrCodeUrl(qrCodeUrl)
            .manualEntryKey(manualEntryKey)
            .message("Scan QR code with Google Authenticator or enter key manually")
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Verify MFA code
     */
    @PostMapping("/mfa/verify")
    public ResponseEntity<String> verifyMfa(@Valid @RequestBody MfaRequest request) {
        boolean isValid = mfaService.verifyCode(request.getUsername(), request.getCode());
        if (isValid) {
            return ResponseEntity.ok("{\"status\":\"success\",\"message\":\"MFA code verified\"}");
        }
        return ResponseEntity.status(401).body("{\"status\":\"error\",\"message\":\"Invalid MFA code\"}");
    }
    
    /**
     * Disable MFA cho user
     */
    @PostMapping("/mfa/disable")
    public ResponseEntity<String> disableMfa(@RequestParam String username) {
        mfaService.disableMfa(username);
        return ResponseEntity.ok("{\"status\":\"success\",\"message\":\"MFA disabled\"}");
    }
    
    /**
     * Check if MFA is enabled for user
     */
    @GetMapping("/mfa/status")
    public ResponseEntity<String> checkMfaStatus(@RequestParam String username) {
        boolean enabled = mfaService.isMfaEnabled(username);
        return ResponseEntity.ok(String.format("{\"enabled\":%s}", enabled));
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
                "user", user
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Đổi password - Yêu cầu authentication
     * User tự đổi password của mình
     */
    @PostMapping("/password/change")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        try {
            // Lấy username từ request body
            String username = request.getUsername();
            
            boolean success = userService.changePassword(username, request);
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Password changed successfully"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Failed to change password"
                ));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
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

