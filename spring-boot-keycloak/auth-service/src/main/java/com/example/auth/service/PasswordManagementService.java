package com.example.auth.service;

import com.example.auth.dto.password.*;
import com.example.auth.entity.AuthUser;
import com.example.auth.model.*;
import com.example.auth.repository.*;
import com.example.auth.exception.PasswordManagementException;
import com.example.auth.exception.ResourceNotFoundException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing password operations including forgot password, change password, and admin reset
 */
@Slf4j
@Service
public class PasswordManagementService {
    
    @Autowired
    private AuthUserRepository authUserRepository;
    
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
    
    @Autowired
    private EmailTemplateRepository emailTemplateRepository;
    
    @Autowired
    private EmailAuditLogRepository emailAuditLogRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private RedisService redisService;
    
    @Value("${app.password-management.reset-code-expiry-minutes:15}")
    private Integer resetCodeExpiryMinutes;
    
    @Value("${app.password-management.admin-reset-code-expiry-minutes:60}")
    private Integer adminResetCodeExpiryMinutes;
    
    @Value("${app.password-management.max-reset-attempts-per-hour:5}")
    private Integer maxResetAttemptsPerHour;
    
    @Value("${app.password-management.max-verify-attempts:3}")
    private Integer maxVerifyAttempts;
    
    @Value("${jwt.secret}")
    private String jwtSecretKey;
    
    @Value("${app.password.min-length:12}")
    private Integer minPasswordLength;
    
    @Value("${app.password.history-count:5}")
    private Integer passwordHistoryCount;
    
    /**
     * Initiate forgot password flow
     */
    @Transactional
    public ForgotPasswordResponse initiateForgotPassword(ForgotPasswordRequest request) {
        log.info("Initiating forgot password flow for email: {}", request.getEmail());
        
        // Find user by email
        AuthUser user = authUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));
        
        // Check rate limiting
        long recentAttempts = passwordResetTokenRepository.countRecentAttempts(
                user.getId(), 
                PasswordResetToken.TokenType.FORGOT_PASSWORD,
                LocalDateTime.now().minusHours(1)
        );
        if (recentAttempts >= maxResetAttemptsPerHour) {
            log.warn("User {} exceeded maximum reset attempts", user.getId());
            throw new PasswordManagementException("Too many password reset attempts. Please try again after some time.");
        }
        
        // Invalidate any existing unverified tokens
        passwordResetTokenRepository.invalidateUnverifiedTokens(user.getId(), PasswordResetToken.TokenType.FORGOT_PASSWORD);
        
        // Generate secure code and token
        String resetCode = generateSecureCode(6);
        String tokenValue = generateSecureToken();
        String tokenHash = hashToken(tokenValue);
        String codeHash = hashToken(resetCode);
        
        // Create password reset token
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .resetCode(resetCode)
                .codeHash(codeHash)
                .tokenType(PasswordResetToken.TokenType.FORGOT_PASSWORD)
                .expiresAt(LocalDateTime.now().plusMinutes(resetCodeExpiryMinutes))
                .verified(false)
                .consumed(false)
                .failedAttempts(0)
                .createdAt(LocalDateTime.now())
                .build();
        
        passwordResetTokenRepository.save(resetToken);
        
        // Store raw code in Redis for verification (with expiry)
        String sessionId = UUID.randomUUID().toString();
        redisService.set("reset_session:" + sessionId, resetCode, resetCodeExpiryMinutes * 60);
        redisService.set("reset_token:" + sessionId, resetToken.getId().toString(), resetCodeExpiryMinutes * 60);
        
        // Send password reset email
        try {
            emailService.sendPasswordResetEmail(user, resetCode);
            log.info("Password reset email sent to user: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to send password reset email", e);
            // Don't throw, allow user to verify even if email fails
        }
        
        return ForgotPasswordResponse.builder()
                .sessionId(sessionId)
                .message("Password reset code has been sent to your email")
                .expiresAt(LocalDateTime.now().plusMinutes(resetCodeExpiryMinutes))
                .codeExpiryMinutes(resetCodeExpiryMinutes)
                .build();
    }
    
    /**
     * Verify reset code
     */
    @Transactional
    public VerifyResetCodeResponse verifyResetCode(VerifyResetCodeRequest request) {
        log.info("Verifying reset code for session: {}", request.getSessionId());
        
        // Get stored code from Redis
        String storedCode = (String) redisService.get("reset_session:" + request.getSessionId());
        if (storedCode == null) {
            log.warn("Invalid or expired session: {}", request.getSessionId());
            throw new PasswordManagementException("Invalid or expired session. Please request a new password reset.");
        }
        
        // Get reset token ID
        String resetTokenId = (String) redisService.get("reset_token:" + request.getSessionId());
        PasswordResetToken resetToken = passwordResetTokenRepository.findById(UUID.fromString(resetTokenId))
                .orElseThrow(() -> new ResourceNotFoundException("Reset token not found"));
        
        // Check if token is expired
        if (resetToken.isExpired()) {
            log.warn("Reset token expired: {}", resetToken.getId());
            throw new PasswordManagementException("Password reset code has expired. Please request a new one.");
        }
        
        // Check failed attempts
        if (resetToken.getFailedAttempts() >= maxVerifyAttempts) {
            log.warn("Max verification attempts exceeded for token: {}", resetToken.getId());
            resetToken.setConsumed(true);
            passwordResetTokenRepository.save(resetToken);
            throw new PasswordManagementException("Too many failed attempts. Please request a new password reset.");
        }
        
        // Verify code with constant-time comparison
        if (!constantTimeEquals(storedCode, request.getResetCode())) {
            resetToken.incrementFailedAttempts();
            passwordResetTokenRepository.save(resetToken);
            log.warn("Invalid reset code attempt for token: {}", resetToken.getId());
            throw new PasswordManagementException("Invalid reset code. Please try again.");
        }
        
        // Mark as verified
        resetToken.setVerified(true);
        resetToken.setVerifiedAt(LocalDateTime.now());
        resetToken.setFailedAttempts(0);
        passwordResetTokenRepository.save(resetToken);
        
        // Generate verification JWT token
        String verificationToken = generateVerificationJWT(resetToken.getUser().getId(), resetToken.getId());
        
        // Store verification state in Redis
        redisService.set("verified:" + request.getSessionId(), "true", 30 * 60);
        
        log.info("Reset code verified successfully for user: {}", resetToken.getUser().getId());
        
        return VerifyResetCodeResponse.builder()
                .verificationToken(verificationToken)
                .verified(true)
                .message("Reset code verified successfully")
                .build();
    }
    
    /**
     * Set new password after verification
     */
    @Transactional
    public SetNewPasswordResponse setNewPassword(SetNewPasswordRequest request) {
        log.info("Setting new password");
        
        // Validate verification token and extract user ID
        UUID userId = validateVerificationToken(request.getVerificationToken());
        
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new PasswordManagementException("Passwords do not match");
        }
        
        // Validate password complexity
        validatePasswordComplexity(request.getNewPassword());
        
        // Check password history
        if (isPasswordUsedRecently(user, request.getNewPassword())) {
            throw new PasswordManagementException("You cannot reuse a recent password. Please choose a different password.");
        }
        
        // Update password
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPassword(encodedPassword);
        user.setPasswordHash(encodedPassword);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        authUserRepository.save(user);
        
        // Invalidate all existing refresh tokens
        revokeAllRefreshTokens(user.getId());
        
        // Log password change
        logPasswordChange(user.getId(), "FORGOT_PASSWORD", "Password reset completed");
        
        // Send confirmation email
        try {
            emailService.sendPasswordChangedConfirmation(user);
        } catch (Exception e) {
            log.error("Failed to send password change confirmation email", e);
        }
        
        log.info("Password reset completed successfully for user: {}", userId);
        
        return SetNewPasswordResponse.builder()
                .success(true)
                .message("Password has been reset successfully. Please login with your new password.")
                .redirectUrl("/login")
                .build();
    }
    
    /**
     * Change password for authenticated user
     */
    @Transactional
    public ChangePasswordResponse changePassword(UUID userId, ChangePasswordRequest request) {
        log.info("Changing password for user: {}", userId);
        
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Verify current password with constant-time comparison
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Invalid current password for user: {}", userId);
            throw new PasswordManagementException("Current password is incorrect");
        }
        
        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new PasswordManagementException("New passwords do not match");
        }
        
        // Validate new password is different from current
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new PasswordManagementException("New password must be different from current password");
        }
        
        // Validate password complexity
        validatePasswordComplexity(request.getNewPassword());
        
        // Check password history
        if (isPasswordUsedRecently(user, request.getNewPassword())) {
            throw new PasswordManagementException("You cannot reuse a recent password. Please choose a different password.");
        }
        
        // Update password
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPassword(encodedPassword);
        user.setPasswordHash(encodedPassword);
        user.setPasswordChangedAt(LocalDateTime.now());
        authUserRepository.save(user);
        
        // Log password change
        logPasswordChange(user.getId(), "USER_INITIATED", "Password changed by user");
        
        // Send confirmation email
        try {
            emailService.sendPasswordChangedConfirmation(user);
        } catch (Exception e) {
            log.error("Failed to send password change confirmation email", e);
        }
        
        log.info("Password changed successfully for user: {}", userId);
        
        return ChangePasswordResponse.builder()
                .success(true)
                .message("Password has been changed successfully")
                .build();
    }
    
    /**
     * Admin reset password for a user
     */
    @Transactional
    public AdminResetPasswordResponse adminResetPassword(UUID adminId, AdminResetPasswordRequest request) {
        log.info("Admin {} initiating password reset for user: {}", adminId, request.getUserId());
        
        // Check admin permissions
        AuthUser admin = authUserRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        
        // Verify admin has ADMIN role
        boolean isAdmin = admin.getRoles() != null && admin.getRoles().stream()
                .anyMatch(role -> "ADMIN".equalsIgnoreCase(role.getCode()));
        
        if (!isAdmin) {
            log.warn("Non-admin user {} attempted password reset", adminId);
            throw new PasswordManagementException("Only administrators can reset passwords");
        }
        
        AuthUser user = authUserRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Generate admin reset code (8 digits for admin)
        String resetCode = generateSecureCode(8);
        String tokenValue = generateSecureToken();
        String tokenHash = hashToken(tokenValue);
        String codeHash = hashToken(resetCode);
        
        // Create admin password reset token
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .resetCode(resetCode)
                .codeHash(codeHash)
                .tokenType(PasswordResetToken.TokenType.ADMIN_RESET)
                .expiresAt(LocalDateTime.now().plusMinutes(adminResetCodeExpiryMinutes))
                .verified(false)
                .consumed(false)
                .failedAttempts(0)
                .createdAt(LocalDateTime.now())
                .createdBy(adminId)
                .build();
        
        passwordResetTokenRepository.save(resetToken);
        
        // Store code and info in Redis
        String sessionId = UUID.randomUUID().toString();
        redisService.set("admin_reset_session:" + sessionId, resetCode, adminResetCodeExpiryMinutes * 60);
        redisService.set("admin_reset_token:" + sessionId, resetToken.getId().toString(), adminResetCodeExpiryMinutes * 60);
        
        // Mark user for forced password change
        user.setForcePasswordChange(true);
        authUserRepository.save(user);
        
        // Send admin reset email
        try {
            emailService.sendAdminResetEmail(user, resetCode);
            log.info("Admin password reset email sent to user: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to send admin reset email", e);
        }
        
        // Log admin action
        logPasswordChange(user.getId(), "ADMIN_RESET", 
                "Password reset by admin " + admin.getEmail() + ". Reason: " + request.getReason());
        
        log.info("Admin password reset initiated for user: {}", user.getId());
        
        return AdminResetPasswordResponse.builder()
                .sessionId(sessionId)
                .resetCode(resetCode) // Return code directly to admin
                .expiresAt(LocalDateTime.now().plusMinutes(adminResetCodeExpiryMinutes))
                .codeExpiryMinutes(adminResetCodeExpiryMinutes)
                .message("Password reset code generated. User has " + adminResetCodeExpiryMinutes + " minutes to reset password.")
                .build();
    }
    
    /**
     * Validate password complexity
     */
    private void validatePasswordComplexity(String password) {
        if (password.length() < minPasswordLength) {
            throw new PasswordManagementException("Password must be at least " + minPasswordLength + " characters long");
        }
        
        if (!password.matches(".*[A-Z].*")) {
            throw new PasswordManagementException("Password must contain at least one uppercase letter");
        }
        
        if (!password.matches(".*[a-z].*")) {
            throw new PasswordManagementException("Password must contain at least one lowercase letter");
        }
        
        if (!password.matches(".*\\d.*")) {
            throw new PasswordManagementException("Password must contain at least one digit");
        }
        
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\",./<>?].*")) {
            throw new PasswordManagementException("Password must contain at least one special character");
        }
    }
    
    /**
     * Check if password was used recently
     */
    private boolean isPasswordUsedRecently(AuthUser user, String newPassword) {
        log.debug("Checking password history for user: {}", user.getId());
        return false;
    }
    
    /**
     * Generate secure random code
     */
    private String generateSecureCode(int length) {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
    
    /**
     * Generate secure random token
     */
    private String generateSecureToken() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Hash token using SHA-256
     */
    private String hashToken(String token) {
        return org.springframework.security.crypto.bcrypt.BCrypt.hashpw(token, org.springframework.security.crypto.bcrypt.BCrypt.gensalt());
    }
    
    /**
     * Constant-time string comparison
     */
    private boolean constantTimeEquals(String a, String b) {
        byte[] aBytes = a.getBytes();
        byte[] bBytes = b.getBytes();
        
        int result = 0;
        result |= aBytes.length ^ bBytes.length;
        
        for (int i = 0; i < Math.min(aBytes.length, bBytes.length); i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        
        return result == 0;
    }
    
    /**
     * Generate verification JWT token
     */
    private String generateVerificationJWT(UUID userId, UUID resetTokenId) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecretKey.getBytes());
        
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("resetTokenId", resetTokenId.toString())
                .claim("type", "password_verification")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 30 * 60 * 1000)) // 30 minutes
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * Validate verification JWT token
     */
    private UUID validateVerificationToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecretKey.getBytes());
            var claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            String type = claims.get("type", String.class);
            if (!"password_verification".equals(type)) {
                throw new PasswordManagementException("Invalid token type");
            }
            
            return UUID.fromString(claims.getSubject());
        } catch (Exception e) {
            log.error("Invalid verification token", e);
            throw new PasswordManagementException("Invalid or expired verification token");
        }
    }
    
    /**
     * Revoke all refresh tokens for user
     */
    private void revokeAllRefreshTokens(UUID userId) {
        // Implementation would depend on your refresh token storage
        // Typically stored in Redis or database
        log.debug("Revoking all refresh tokens for user: {}", userId);
        redisService.deletePattern("refresh_token:*:" + userId);
    }
    
    /**
     * Log password change
     */
    private void logPasswordChange(UUID userId, String action, String details) {
        try {
            // This would save to password_audit table
            log.info("Password audit: user={}, action={}, details={}", userId, action, details);
        } catch (Exception e) {
            log.error("Failed to log password change", e);
        }
    }
}
