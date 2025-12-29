package com.example.keycloak.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

/**
 * RSA Service for secure password encryption/decryption
 * Uses RSA/ECB/PKCS1Padding for compatibility with jsencrypt library
 * 
 */
@Slf4j
@Service
public class RsaService {

    private static final int KEY_SIZE = 2048;
    private static final String ALGORITHM = "RSA";
    // PKCS1Padding for jsencrypt compatibility
    private static final String CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding";

    private KeyPair keyPair;
    private String publicKeyBase64;

    @PostConstruct
    public void init() {
        try {
            log.info("Generating RSA key pair ({} bits)...", KEY_SIZE);
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
            keyPairGenerator.initialize(KEY_SIZE, new SecureRandom());
            this.keyPair = keyPairGenerator.generateKeyPair();

            // Cache public key as Base64 for API response
            this.publicKeyBase64 = Base64.getEncoder().encodeToString(
                    keyPair.getPublic().getEncoded());

            log.info("RSA key pair generated successfully");
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate RSA key pair", e);
            throw new RuntimeException("Failed to initialize RSA service", e);
        }
    }

    /**
     * Get public key in Base64 format for frontend
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
     * Decrypt RSA-encrypted password from frontend
     * 
     * @param encryptedBase64 Base64-encoded encrypted password
     * @return Decrypted plain password
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
            throw new RuntimeException("Failed to decrypt password", e);
        }
    }

    /**
     * Encrypt with public key (for testing purposes)
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
     * Check if a string looks like RSA-encrypted data (Base64 encoded)
     */
    public boolean isEncrypted(String value) {
        if (value == null || value.length() < 100) {
            return false;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            // RSA 2048-bit encrypted data is 256 bytes
            return decoded.length == 256;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
