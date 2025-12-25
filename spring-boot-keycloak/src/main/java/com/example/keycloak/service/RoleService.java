package com.example.keycloak.service;

import com.example.keycloak.provider.CustomUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Service để quản lý roles của users
 */
@Service
public class RoleService {

    @Autowired
    private DataSource dataSource;

    /**
     * Cập nhật role cho user
     * @param username Username của user
     * @param role Role mới ('admin' hoặc 'user')
     * @return true nếu thành công
     */
    public boolean updateUserRole(String username, String role) {
        // Validate role
        if (!role.equals("admin") && !role.equals("user")) {
            throw new IllegalArgumentException("Role must be 'admin' or 'user'");
        }

        try (Connection connection = dataSource.getConnection()) {
            CustomUserRepository userRepository = new CustomUserRepository(connection);
            return userRepository.updateUserRole(username, role);
        } catch (SQLException e) {
            throw new RuntimeException("Error updating user role: " + username, e);
        }
    }

    /**
     * Lấy role của user
     * @param username Username của user
     * @return Role của user ('admin' hoặc 'user')
     */
    public String getUserRole(String username) {
        try (Connection connection = dataSource.getConnection()) {
            CustomUserRepository userRepository = new CustomUserRepository(connection);
            var user = userRepository.findByUsername(username);
            if (user == null) {
                return null;
            }
            return user.getRole() != null ? user.getRole() : "user";
        } catch (SQLException e) {
            throw new RuntimeException("Error getting user role: " + username, e);
        }
    }
}

