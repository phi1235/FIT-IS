package com.example.auth.controller;

import com.example.auth.dto.password.*;
import com.example.auth.service.PasswordManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

/**
 * REST Controller for password management endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class PasswordManagementController {
    
    @Autowired
    private PasswordManagementService passwordManagementService;
    
    /**
     * Initiate forgot password flow
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Initiating forgot password for email: {}", request.getEmail());
        ForgotPasswordResponse response = passwordManagementService.initiateForgotPassword(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Verify reset code
     */
    @PostMapping("/verify-reset-code")
    public ResponseEntity<VerifyResetCodeResponse> verifyResetCode(@Valid @RequestBody VerifyResetCodeRequest request) {
        log.info("Verifying reset code for session: {}", request.getSessionId());
        VerifyResetCodeResponse response = passwordManagementService.verifyResetCode(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Set new password after verification
     */
    @PostMapping("/set-new-password")
    public ResponseEntity<SetNewPasswordResponse> setNewPassword(@Valid @RequestBody SetNewPasswordRequest request) {
        log.info("Setting new password");
        SetNewPasswordResponse response = passwordManagementService.setNewPassword(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Change password for authenticated user
     */
    @PostMapping("/change-password")
    public ResponseEntity<ChangePasswordResponse> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        log.info("User {} requesting password change", authentication.getName());
        UUID userId = UUID.fromString(authentication.getPrincipal().toString());
        ChangePasswordResponse response = passwordManagementService.changePassword(userId, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Admin reset password for a user
     */
    @PostMapping("/admin/reset-password")
    public ResponseEntity<AdminResetPasswordResponse> adminResetPassword(
            Authentication authentication,
            @Valid @RequestBody AdminResetPasswordRequest request) {
        log.info("Admin {} requesting password reset for user: {}", authentication.getName(), request.getUserId());
        UUID adminId = UUID.fromString(authentication.getPrincipal().toString());
        AdminResetPasswordResponse response = passwordManagementService.adminResetPassword(adminId, request);
        return ResponseEntity.ok(response);
    }
}
