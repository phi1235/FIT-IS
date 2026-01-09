package com.example.user.controller;

import com.example.common.dto.PagedResponse;
import com.example.common.util.SecurityUtils;
import com.example.user.dto.UserDTO;
import com.example.user.entity.Role;
import com.example.user.entity.User;
import com.example.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final UserRepository userRepository;

    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('admin') or hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<UserDTO>> getUsersPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String search) {

        log.info("Fetching users for admin. Page: {}, Size: {}, Search: '{}'", page, size, search);
        org.springframework.security.core.context.SecurityContext context = org.springframework.security.core.context.SecurityContextHolder.getContext();
        if (context.getAuthentication() != null) {
            log.info("User: {}, Authorities: {}", context.getAuthentication().getName(), context.getAuthentication().getAuthorities());
        } else {
            log.warn("No authentication found in SecurityContext");
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage;
        
        if (search == null || search.trim().isEmpty()) {
            userPage = userRepository.findAll(pageable);
        } else {
            userPage = userRepository.searchUsers(search, pageable);
        }

        Page<UserDTO> dtoPage = userPage.map(this::convertToDTO);

        return ResponseEntity.ok(PagedResponse.<UserDTO>builder()
                .content(dtoPage.getContent())
                .page(dtoPage.getNumber())
                .size(dtoPage.getSize())
                .totalElements(dtoPage.getTotalElements())
                .totalPages(dtoPage.getTotalPages())
                .first(dtoPage.isFirst())
                .last(dtoPage.isLast())
                .build());
    }

    @GetMapping("/admin/role")
    @PreAuthorize("hasRole('admin') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> getUserRole(@RequestParam String username) {
        return userRepository.findByUsername(username)
                .map(user -> {
                    String role = user.getRoles().isEmpty() ? "user" : user.getRoles().iterator().next().getCode();
                    return ResponseEntity.ok(Map.of("username", username, "role", role));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/me/role")
    public ResponseEntity<Map<String, String>> getMyRole() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            return ResponseEntity.status(401).build();
        }
        
        return userRepository.findByUsername(username)
                .map(user -> {
                    String role = user.getRoles().isEmpty() ? "user" : user.getRoles().iterator().next().getCode();
                    return ResponseEntity.ok(Map.of("username", username, "role", role));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private UserDTO convertToDTO(User user) {
        Set<String> roleCodes = user.getRoles().stream()
                .map(Role::getCode)
                .collect(Collectors.toSet());
        
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .roles(roleCodes)
                .role(roleCodes.isEmpty() ? "user" : roleCodes.iterator().next())
                .build();
    }
}
