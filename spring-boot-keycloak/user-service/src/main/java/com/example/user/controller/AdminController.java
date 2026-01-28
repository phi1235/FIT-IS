package com.example.user.controller;

import com.example.common.dto.PagedResponse;
import com.example.common.util.SecurityUtils;
import com.example.user.dto.UserDTO;
import com.example.user.entity.Role;
import com.example.user.entity.User;
import com.example.user.repository.RoleRepository;
import com.example.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @GetMapping("/admin/list")
    @PreAuthorize("hasAuthority('USER_VIEW') or hasAuthority('USER_MANAGE')")
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
    @PreAuthorize("hasAuthority('USER_VIEW') or hasAuthority('USER_MANAGE')")
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

    @PostMapping("/admin/role")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @Transactional
    public ResponseEntity<String> updateUserRole(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String roleCode = request.get("role");

        log.info("Updating role for user {}: {}", username, roleCode);

        return userRepository.findByUsername(username)
                .map(user -> {
                    Role role = roleRepository.findByCode(roleCode)
                            .orElseThrow(() -> new RuntimeException("Role not found: " + roleCode));
                    
                    user.getRoles().clear();
                    user.getRoles().add(role);
                    userRepository.save(user);
                    return ResponseEntity.ok("Role updated successfully");
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found"));
    }

    @PostMapping("/admin/create")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @Transactional
    public ResponseEntity<?> createUser(@RequestBody UserDTO dto) {
        log.info("Creating user: {}", dto.getUsername());
        
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        Role userRole = roleRepository.findByCode(dto.getRole() != null ? dto.getRole() : "user")
                .orElseThrow(() -> new RuntimeException("Default role not found"));

        User user = User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .enabled(true)
                .passwordHash(passwordEncoder.encode("Default123@")) // Set a default password
                .roles(new HashSet<>(java.util.Set.of(userRole)))
                .build();

        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(user));
    }

    @PutMapping("/admin/update/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @Transactional
    public ResponseEntity<?> updateUser(@PathVariable UUID id, @RequestBody UserDTO dto) {
        log.info("Updating user id: {}", id);
        
        return userRepository.findById(id)
                .map(user -> {
                    user.setEmail(dto.getEmail());
                    user.setFirstName(dto.getFirstName());
                    user.setLastName(dto.getLastName());
                    
                    if (dto.getRole() != null) {
                        Role role = roleRepository.findByCode(dto.getRole())
                                .orElseThrow(() -> new RuntimeException("Role not found: " + dto.getRole()));
                        user.getRoles().clear();
                        user.getRoles().add(role);
                    }
                    
                    userRepository.save(user);
                    return ResponseEntity.ok(convertToDTO(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/admin/delete/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        log.info("Deleting user id: {}", id);
        
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/lock/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @Transactional
    public ResponseEntity<?> lockUser(@PathVariable UUID id) {
        log.info("Locking user id: {}", id);
        
        return userRepository.findById(id)
                .map(user -> {
                    user.setEnabled(false);
                    userRepository.save(user);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/admin/unlock/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @Transactional
    public ResponseEntity<?> unlockUser(@PathVariable UUID id) {
        log.info("Unlocking user id: {}", id);
        
        return userRepository.findById(id)
                .map(user -> {
                    user.setEnabled(true);
                    userRepository.save(user);
                    return ResponseEntity.ok().build();
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
