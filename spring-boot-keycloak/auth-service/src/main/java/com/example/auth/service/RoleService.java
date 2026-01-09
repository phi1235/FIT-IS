package com.example.auth.service;

import com.example.auth.dto.RoleDTO;
import com.example.auth.entity.Permission;
import com.example.auth.entity.Role;
import com.example.auth.repository.PermissionRepository;
import com.example.auth.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Role Service for RBAC CRUD operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    /**
     * Get all roles
     */
    public List<RoleDTO> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get role by ID
     */
    public RoleDTO getRoleById(UUID id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found: " + id));
        return toDTO(role);
    }

    /**
     * Get role by code
     */
    public RoleDTO getRoleByCode(String code) {
        Role role = roleRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Role not found: " + code));
        return toDTO(role);
    }

    /**
     * Create new role
     */
    @Transactional
    public RoleDTO createRole(RoleDTO dto) {
        if (roleRepository.existsByCode(dto.getCode())) {
            throw new RuntimeException("Role code already exists: " + dto.getCode());
        }

        Role role = Role.builder()
                .code(dto.getCode().toUpperCase())
                .name(dto.getName())
                .description(dto.getDescription())
                .isSystem(false)
                .permissions(new HashSet<>())
                .build();

        // Add permissions if provided
        if (dto.getPermissionCodes() != null && !dto.getPermissionCodes().isEmpty()) {
            Set<Permission> permissions = dto.getPermissionCodes().stream()
                    .map(code -> permissionRepository.findByCode(code)
                            .orElseThrow(() -> new RuntimeException("Permission not found: " + code)))
                    .collect(Collectors.toSet());
            role.setPermissions(permissions);
        }

        role = roleRepository.save(role);
        log.info("Role created: {}", role.getCode());
        return toDTO(role);
    }

    /**
     * Update existing role
     */
    @Transactional
    public RoleDTO updateRole(UUID id, RoleDTO dto) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found: " + id));

        if (role.isSystem()) {
            throw new RuntimeException("Cannot modify system role: " + role.getCode());
        }

        role.setName(dto.getName());
        role.setDescription(dto.getDescription());

        // Update permissions if provided
        if (dto.getPermissionCodes() != null) {
            Set<Permission> permissions = dto.getPermissionCodes().stream()
                    .map(code -> permissionRepository.findByCode(code)
                            .orElseThrow(() -> new RuntimeException("Permission not found: " + code)))
                    .collect(Collectors.toSet());
            role.setPermissions(permissions);
        }

        role = roleRepository.save(role);
        log.info("Role updated: {}", role.getCode());
        return toDTO(role);
    }

    /**
     * Delete role
     */
    @Transactional
    public void deleteRole(UUID id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found: " + id));

        if (role.isSystem()) {
            throw new RuntimeException("Cannot delete system role: " + role.getCode());
        }

        roleRepository.delete(role);
        log.info("Role deleted: {}", role.getCode());
    }

    /**
     * Add permission to role
     */
    @Transactional
    public RoleDTO addPermissionToRole(UUID roleId, String permissionCode) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));

        Permission permission = permissionRepository.findByCode(permissionCode)
                .orElseThrow(() -> new RuntimeException("Permission not found: " + permissionCode));

        role.getPermissions().add(permission);
        role = roleRepository.save(role);
        log.info("Permission {} added to role {}", permissionCode, role.getCode());
        return toDTO(role);
    }

    /**
     * Remove permission from role
     */
    @Transactional
    public RoleDTO removePermissionFromRole(UUID roleId, String permissionCode) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));

        role.getPermissions().removeIf(p -> p.getCode().equals(permissionCode));
        role = roleRepository.save(role);
        log.info("Permission {} removed from role {}", permissionCode, role.getCode());
        return toDTO(role);
    }

    private RoleDTO toDTO(Role role) {
        Set<String> permissionCodes = role.getPermissions() != null
                ? role.getPermissions().stream().map(Permission::getCode).collect(Collectors.toSet())
                : new HashSet<>();

        return RoleDTO.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .description(role.getDescription())
                .isSystem(role.isSystem())
                .permissionCodes(permissionCodes)
                .build();
    }
}
