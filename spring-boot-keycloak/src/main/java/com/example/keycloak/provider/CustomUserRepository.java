package com.example.keycloak.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Repository để truy vấn users từ custom database
 */
public class CustomUserRepository {

    private final java.sql.Connection connection;

    public CustomUserRepository(java.sql.Connection connection) {
        this.connection = connection;
    }

    public CustomUser findByUsername(String username) {
        String sql = "SELECT id, username, email, password, first_name, last_name, enabled, role, COALESCE(password_version, 1) as password_version FROM users WHERE username = ?";
        try (java.sql.PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Error fetching user by username: " + username, e);
        }
        return null;
    }

    public CustomUser findByEmail(String email) {
        String sql = "SELECT id, username, email, password, first_name, last_name, enabled, role, COALESCE(password_version, 1) as password_version FROM users WHERE email = ?";
        try (java.sql.PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Error fetching user by email: " + email, e);
        }
        return null;
    }

    public CustomUser findById(String id) {
        String sql = "SELECT id, username, email, password, first_name, last_name, enabled, role, COALESCE(password_version, 1) as password_version FROM users WHERE id = ?";
        try (java.sql.PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Error fetching user by id: " + id, e);
        }
        return null;
    }

    public List<CustomUser> searchUsers(String search) {
        String sql = "SELECT id, username, email, password, first_name, last_name, enabled, role FROM users WHERE " +
                "LOWER(username) LIKE ? OR LOWER(email) LIKE ? OR LOWER(first_name) LIKE ? OR LOWER(last_name) LIKE ?";
        List<CustomUser> result = new ArrayList<>();
        try (java.sql.PreparedStatement pstmt = connection.prepareStatement(sql)) {
            String query = "%" + (search == null ? "" : search.toLowerCase()) + "%";
            pstmt.setString(1, query);
            pstmt.setString(2, query);
            pstmt.setString(3, query);
            pstmt.setString(4, query);

            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapUser(rs));
                }
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Error searching users with query: " + search, e);
        }
        return result;
    }

    /**
     * Search users with pagination (LIMIT/OFFSET)
     * 
     * @param search search query (can be null or empty)
     * @param page   page number (0-based)
     * @param size   page size
     */
    public List<CustomUser> searchUsersPaginated(String search, int page, int size) {
        String sql = "SELECT id, username, email, password, first_name, last_name, enabled, role FROM users WHERE " +
                "LOWER(username) LIKE ? OR LOWER(email) LIKE ? OR LOWER(first_name) LIKE ? OR LOWER(last_name) LIKE ? "
                +
                "ORDER BY username LIMIT ? OFFSET ?";
        List<CustomUser> result = new ArrayList<>();
        try (java.sql.PreparedStatement pstmt = connection.prepareStatement(sql)) {
            String query = "%" + (search == null ? "" : search.toLowerCase()) + "%";
            pstmt.setString(1, query);
            pstmt.setString(2, query);
            pstmt.setString(3, query);
            pstmt.setString(4, query);
            pstmt.setInt(5, size);
            pstmt.setInt(6, page * size);

            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapUser(rs));
                }
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Error searching users with pagination", e);
        }
        return result;
    }

    /**
     * Count total users matching search query
     */
    public int countBySearch(String search) {
        String sql = "SELECT COUNT(*) FROM users WHERE " +
                "LOWER(username) LIKE ? OR LOWER(email) LIKE ? OR LOWER(first_name) LIKE ? OR LOWER(last_name) LIKE ?";
        try (java.sql.PreparedStatement pstmt = connection.prepareStatement(sql)) {
            String query = "%" + (search == null ? "" : search.toLowerCase()) + "%";
            pstmt.setString(1, query);
            pstmt.setString(2, query);
            pstmt.setString(3, query);
            pstmt.setString(4, query);
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Error counting users by search", e);
        }
        return 0;
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM users";
        try (java.sql.Statement stmt = connection.createStatement();
                java.sql.ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Error counting users", e);
        }
        return 0;
    }

    private CustomUser mapUser(java.sql.ResultSet rs) throws java.sql.SQLException {
        String role = rs.getString("role");
        if (role == null) {
            role = "user"; // Default role
        }
        String password = rs.getString("password");
        // Debug: Log password hash length to ensure it's not truncated
        if (password != null && password.length() < 60) {
            System.err.println(
                    "WARNING: Password hash seems truncated! Length: " + password.length() + ", Hash: " + password);
        }

        // Get password version, default to 1 (old format) if column doesn't exist
        int passwordVersion = 1;
        try {
            passwordVersion = rs.getInt("password_version");
            if (rs.wasNull()) {
                passwordVersion = 1;
            }
        } catch (java.sql.SQLException e) {
            // Column doesn't exist, use default
        }

        CustomUser user = new CustomUser(
                rs.getString("id"),
                rs.getString("username"),
                rs.getString("email"),
                password,
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getBoolean("enabled"),
                role);
        user.setPasswordVersion(passwordVersion);
        return user;
    }

    /**
     * Update user role
     */
    public boolean updateUserRole(String username, String role) {
        String sql = "UPDATE users SET role = ? WHERE username = ?";
        try (java.sql.PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, role);
            pstmt.setString(2, username);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Error updating user role: " + username, e);
        }
    }

    /**
     * Update password with new hash and version
     * Used during password migration from old to new format
     */
    public boolean updatePasswordWithVersion(String username, String hashedPassword, int version) {
        String sql = "UPDATE users SET password = ?, password_version = ? WHERE username = ?";
        try (java.sql.PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, hashedPassword);
            pstmt.setInt(2, version);
            pstmt.setString(3, username);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Error updating password for user: " + username, e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (java.sql.SQLException e) {
            // Ignore or log
        }
    }
}
