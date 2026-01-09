package com.example.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

/**
 * RSA Service for encryption/decryption
 * 
 * Supports two formats:
 * 1. Single value: Just encrypted password/username
 * 2. Combined format: KEY|PASSWORD|USERNAME or USERNAME|PASSWORD|KEY (reversed)
 */
@Slf4j
@Service
public class RsaService {

    private static final int KEY_SIZE = 2048;
    private static final String ALGORITHM = "RSA";
    private static final String CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding";
    
    // Delimiter to separate parts in combined format
    private static final String DELIMITER = "\\|";

    private KeyPair keyPair;
    private String publicKeyBase64;

    @PostConstruct
    public void init() {
        try {
            log.info("Generating RSA key pair ({} bits)...", KEY_SIZE);
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
            keyPairGenerator.initialize(KEY_SIZE, new SecureRandom());
            this.keyPair = keyPairGenerator.generateKeyPair();

            this.publicKeyBase64 = Base64.getEncoder().encodeToString(
                    keyPair.getPublic().getEncoded());

            log.info("RSA key pair generated successfully");
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate RSA key pair", e);
            throw new RuntimeException("Failed to initialize RSA service", e);
        }
    }

    /**
     * Get public key in Base64 format
     */
    public String getPublicKeyBase64() {
        return publicKeyBase64;
    }

    /**
     * Get public key in PEM format for frontend compatibility
     */
    public String getPublicKeyPem() {
        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN PUBLIC KEY-----\n");

        String base64 = publicKeyBase64;
        int index = 0;
        while (index < base64.length()) {
            pem.append(base64, index, Math.min(index + 64, base64.length()));
            pem.append("\n");
            index += 64;
        }

        pem.append("-----END PUBLIC KEY-----");
        return pem.toString();
    }

    /**
     * Decrypt RSA-encrypted data
     */
    public String decrypt(String encryptedBase64) {
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedBase64);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("RSA decryption failed: {}", e.getMessage());
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }

    /**
     * Decrypt and parse combined credentials format
     * Enforces standard format: KEY|PASSWORD|USERNAME
     * 
     * @param encryptedBase64 Encrypted combined credentials
     * @return ParsedCredentials with username and password
     */
    public ParsedCredentials decryptCredentials(String encryptedBase64) {
        String decrypted = decrypt(encryptedBase64);
        String[] parts = decrypted.split(DELIMITER);

        if (parts.length != 3) {
            log.warn("Invalid credential format, expected 3 parts but got {}", parts.length);
            throw new RuntimeException("Invalid credential format");
        }

        // Enforced standard format: [0]=KEY, [1]=PASSWORD, [2]=USERNAME
        String password = parts[1];
        String username = parts[2];
        
        log.debug("Parsed credentials in standard format (KEY|PASSWORD|USERNAME)");

        return new ParsedCredentials(username, password);
    }

    /**
     * Encrypt with public key (for testing)
     */
    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());

            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("RSA encryption failed: {}", e.getMessage());
            throw new RuntimeException("Failed to encrypt", e);
        }
    }

    /**
     * Check if a string looks like RSA-encrypted data
     */
    public boolean isEncrypted(String value) {
        if (value == null || value.length() < 100) {
            return false;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            return decoded.length == 256;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Check if encrypted data contains combined credentials (KEY|PASS|USER format)
     */
    public boolean isCombinedCredentials(String encryptedBase64) {
        try {
            String decrypted = decrypt(encryptedBase64);
            String[] parts = decrypted.split(DELIMITER);
            return parts.length == 3;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Parsed credentials DTO
     */
    public static class ParsedCredentials {
        private final String username;
        private final String password;

        public ParsedCredentials(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }
}
