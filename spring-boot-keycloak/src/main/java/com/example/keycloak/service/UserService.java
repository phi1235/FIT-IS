package com.example.keycloak.service;

import com.example.keycloak.dto.ChangePasswordRequest;
import com.example.keycloak.dto.RegisterRequest;
import com.example.keycloak.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Service xử lý đăng ký và đổi password
 */
@Slf4j
@Service
public class UserService {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    private final PasswordEncoder passwordEncoder;

    public UserService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Đăng ký user mới
     */
    public UserDTO registerUser(RegisterRequest request) {
        try (Connection conn = getConnection()) {
            // Check if username already exists
            if (usernameExists(conn, request.getUsername())) {
                throw new RuntimeException("Username already exists");
            }

            // Check if email already exists
            if (emailExists(conn, request.getEmail())) {
                throw new RuntimeException("Email already exists");
            }

            // Hash password với BCrypt
            String hashedPassword = passwordEncoder.encode(request.getPassword());

            // Generate UUID for new user
            String id = UUID.randomUUID().toString();

            // Insert new user
            String sql = "INSERT INTO users (id, username, email, password, first_name, last_name, enabled, role) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, id);
                pstmt.setString(2, request.getUsername().trim().toLowerCase());
                pstmt.setString(3, request.getEmail().trim().toLowerCase());
                pstmt.setString(4, hashedPassword);
                pstmt.setString(5, request.getFirstName());
                pstmt.setString(6, request.getLastName());
                pstmt.setBoolean(7, true); // enabled by default
                pstmt.setString(8, "user"); // default role

                pstmt.executeUpdate();
            }

            log.info("User registered successfully: {}", request.getUsername());

            return new UserDTO(
                    id,
                    request.getUsername(),
                    request.getEmail(),
                    request.getFirstName(),
                    request.getLastName(),
                    "user",
                    true);
        } catch (SQLException e) {
            log.error("Error registering user: {}", e.getMessage());
            throw new RuntimeException("Failed to register user: " + e.getMessage());
        }
    }

    /**
     * Đổi password cho user
     */
    public boolean changePassword(String username, ChangePasswordRequest request) {
        try (Connection conn = getConnection()) {
            // Get current password hash
            String sql = "SELECT password FROM users WHERE username = ?";
            String currentHash = null;

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        currentHash = rs.getString("password");
                    }
                }
            }

            if (currentHash == null) {
                throw new RuntimeException("User not found");
            }

            // Verify current password
            if (!passwordEncoder.matches(request.getCurrentPassword(), currentHash)) {
                throw new RuntimeException("Current password is incorrect");
            }

            // Hash and update new password
            String newHash = passwordEncoder.encode(request.getNewPassword());
            String updateSql = "UPDATE users SET password = ? WHERE username = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setString(1, newHash);
                pstmt.setString(2, username);
                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    log.info("Password changed successfully for user: {}", username);
                    return true;
                }
            }

            return false;
        } catch (SQLException e) {
            log.error("Error changing password: {}", e.getMessage());
            throw new RuntimeException("Failed to change password: " + e.getMessage());
        }
    }

    private boolean usernameExists(Connection conn, String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE LOWER(username) = LOWER(?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean emailExists(Connection conn, String email) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE LOWER(email) = LOWER(?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Tự động migrate password từ version 1 sang version 2 khi login
     * Không cần user đổi password, chỉ cần hash password hiện tại bằng SHA256 và update
     * 
     * @param username        username
     * @param plainPassword   plain password đã được verify (match với current hash)
     * @return true nếu migrate thành công
     */
    public boolean autoMigratePassword(String username, String plainPassword) {        try (Connection conn = getConnection()) {
            // Get current password hash and version
            String sql = "SELECT password, COALESCE(password_version, 1) as password_version FROM users WHERE username = ?";
            String currentHash = null;
            int currentVersion = 1;

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        currentHash = rs.getString("password");
                        currentVersion = rs.getInt("password_version");
                    }
                }
            }            if (currentHash == null) {
                throw new RuntimeException("User not found");
            }

            // Chỉ migrate nếu đang ở version 1
            if (currentVersion != 1) {
                log.debug("User {} already has password version {}, skipping auto-migration", username, currentVersion);
                return false;
            }

            // Hash plain password bằng SHA256 (client-side hashing simulation)
            String sha256Hash = sha256(plainPassword);            // Hash SHA256 result bằng BCrypt và update với version 2
            String newHash = passwordEncoder.encode(sha256Hash);
            String updateSql = "UPDATE users SET password = ?, password_version = 2 WHERE username = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setString(1, newHash);
                pstmt.setString(2, username);
                int rowsAffected = pstmt.executeUpdate();                if (rowsAffected > 0) {
                    log.info("Password auto-migrated successfully for user: {} (version 1 -> 2)", username);                    return true;
                }
            }

            return false;
        } catch (SQLException e) {
            log.error("Error auto-migrating password: {}", e.getMessage());
            throw new RuntimeException("Failed to auto-migrate password: " + e.getMessage());
        }
    }

    /**
     * SHA256 hash helper
     */
    private String sha256(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA256 hashing failed", e);
        }
    }

    /**
     * Migrate password from old format (version 1) to new SHA256 format (version 2)
     * 
     * @param username          username
     * @param currentPassword   plain password for verification against current hash
     * @param newPasswordSha256 SHA256 hashed new password from client
     */
    public boolean migratePassword(String username, String currentPassword, String newPasswordSha256) {
        try (Connection conn = getConnection()) {
            // Get current password hash and version
            String sql = "SELECT password, COALESCE(password_version, 1) as password_version FROM users WHERE username = ?";
            String currentHash = null;
            int currentVersion = 1;

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        currentHash = rs.getString("password");
                        currentVersion = rs.getInt("password_version");
                    }
                }
            }

            if (currentHash == null) {
                throw new RuntimeException("User not found");
            }

            // Only allow migration for version 1 users
            if (currentVersion != 1) {
                throw new RuntimeException("Password is already migrated to new format");
            }

            // Verify current password (plain password against BCrypt hash)
            if (!passwordEncoder.matches(currentPassword, currentHash)) {
                throw new RuntimeException("Current password is incorrect");
            }

            // Validate SHA256 input (should be 64 characters hex)
            if (newPasswordSha256 == null || newPasswordSha256.length() != 64) {
                throw new RuntimeException("Invalid new password format. Expected SHA256 hash.");
            }

            // Hash the SHA256 password with BCrypt and update with version 2
            String newHash = passwordEncoder.encode(newPasswordSha256);
            String updateSql = "UPDATE users SET password = ?, password_version = 2 WHERE username = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setString(1, newHash);
                pstmt.setString(2, username);
                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    log.info("Password migrated successfully for user: {} (version 1 -> 2)", username);
                    return true;
                }
            }

            return false;
        } catch (SQLException e) {
            log.error("Error migrating password: {}", e.getMessage());
            throw new RuntimeException("Failed to migrate password: " + e.getMessage());
        }
    }

    private Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL driver not found");
        }
        return DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
    }
}
