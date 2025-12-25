package com.example.keycloak.service;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-Factor Authentication (MFA) Service sử dụng TOTP (Time-based One-Time Password)
 * 
 * Bank-Level Security:
 * - TOTP với 6 digits, 30 seconds window
 * - QR code generation cho Google Authenticator, Authy, etc.
 * - Secret key generation và validation
 */
@Slf4j
@Service
public class MfaService {
    
    // Lưu trữ secret keys cho mỗi user (trong production nên lưu vào database)
    private final Map<String, String> userSecrets = new ConcurrentHashMap<>();
    
    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    
    /**
     * Tạo secret key mới cho user và generate QR code
     */
    public String generateSecret(String username) {
        String secret = secretGenerator.generate();
        userSecrets.put(username, secret);
        log.info("MFA_SECRET_GENERATED | user={}", username);
        return secret;
    }
    
    /**
     * Generate QR code URL cho Google Authenticator
     */
    public String generateQrCodeUrl(String username, String secret, String issuer) {
        QrData data = new QrData.Builder()
            .label(username)
            .secret(secret)
            .issuer(issuer)
            .algorithm(HashingAlgorithm.SHA1)
            .digits(6)
            .period(30)
            .build();
        
        try {
            QrGenerator qrGenerator = new ZxingPngQrGenerator();
            byte[] qrCode = qrGenerator.generate(data);
            return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(qrCode);
        } catch (QrGenerationException e) {
            log.error("Error generating QR code for user {}: {}", username, e.getMessage());
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }
    
    /**
     * Verify TOTP code từ user
     */
    public boolean verifyCode(String username, String code) {
        String secret = userSecrets.get(username);
        if (secret == null) {
            log.warn("MFA_VERIFY_FAILED | user={} | reason=NO_SECRET", username);
            return false;
        }
        
        try {
            boolean isValid = codeVerifier.isValidCode(secret, code);
            if (isValid) {
                log.info("MFA_VERIFY_SUCCESS | user={}", username);
            } else {
                log.warn("MFA_VERIFY_FAILED | user={} | reason=INVALID_CODE", username);
            }
            return isValid;
        } catch (Exception e) {
            log.error("MFA_VERIFY_ERROR | user={} | error={}", username, e.getMessage());
            return false;
        }
    }
    
    /**
     * Kiểm tra user đã setup MFA chưa
     */
    public boolean isMfaEnabled(String username) {
        return userSecrets.containsKey(username);
    }
    
    /**
     * Disable MFA cho user
     */
    public void disableMfa(String username) {
        userSecrets.remove(username);
        log.info("MFA_DISABLED | user={}", username);
    }
    
    /**
     * Get secret cho user (để hiển thị manual entry code)
     */
    public String getSecret(String username) {
        return userSecrets.get(username);
    }
}

