package com.example.keycloak.controller;

import com.example.keycloak.dto.RoleUpdateRequest;
import com.example.keycloak.dto.UserDTO;
import com.example.keycloak.dto.UserInfo;
import com.example.keycloak.provider.CustomUser;
import com.example.keycloak.provider.CustomUserRepository;
import com.example.keycloak.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import javax.validation.Valid;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

    @Autowired
    private RoleService roleService;

    @Autowired
    private DataSource dataSource;

    /**
     * Lấy thông tin user hiện tại từ JWT token
     */
    @GetMapping("/me")
    public ResponseEntity<UserInfo> getCurrentUser(Authentication authentication) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUsername(authentication.getName());
        userInfo.setAuthorities(authentication.getAuthorities().toString());
        return ResponseEntity.ok(userInfo);
    }

    /**
     * Admin endpoint - Lấy danh sách tất cả users từ database
     * Chỉ user có role admin mới truy cập được
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        try (Connection connection = dataSource.getConnection()) {
            CustomUserRepository userRepository = new CustomUserRepository(connection);
            List<CustomUser> users = userRepository.searchUsers(""); // Search empty để lấy tất cả
            
            List<UserDTO> userDTOs = users.stream()
                    .map(user -> new UserDTO(
                            user.getId(),
                            user.getUsername(),
                            user.getEmail(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getRole() != null ? user.getRole() : "user",
                            user.isEnabled()
                    ))
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(userDTOs);
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Admin endpoint - Cập nhật role cho user
     * Chỉ admin mới có quyền thay đổi role
     */
    @PostMapping("/admin/role")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<String> updateUserRole(@Valid @RequestBody RoleUpdateRequest request) {
        boolean success = roleService.updateUserRole(request.getUsername(), request.getRole());
        if (success) {
            return ResponseEntity.ok(String.format("Role updated successfully: %s -> %s", 
                    request.getUsername(), request.getRole()));
        }
        return ResponseEntity.badRequest()
                .body(String.format("Failed to update role for user: %s", request.getUsername()));
    }

    /**
     * Admin endpoint - Lấy role của user
     */
    @GetMapping("/admin/role")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<String> getUserRole(@RequestParam String username) {
        String role = roleService.getUserRole(username);
        if (role != null) {
            return ResponseEntity.ok(String.format("{\"username\":\"%s\",\"role\":\"%s\"}", username, role));
        }
        return ResponseEntity.notFound().build();
        }

    /**
     * User endpoint - Lấy role của chính mình
     */
    @GetMapping("/me/role")
    public ResponseEntity<String> getMyRole(Authentication authentication) {
        String username = authentication.getName();
        String role = roleService.getUserRole(username);
        if (role != null) {
            return ResponseEntity.ok(String.format("{\"username\":\"%s\",\"role\":\"%s\"}", username, role));
        }
        return ResponseEntity.ok(String.format("{\"username\":\"%s\",\"role\":\"user\"}", username));
    }
}
