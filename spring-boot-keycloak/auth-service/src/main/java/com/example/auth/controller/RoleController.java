package com.example.auth.controller;

import com.example.auth.dto.RoleDTO;
import com.example.auth.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Role Controller - CRUD operations for RBAC roles
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('admin')")
public class RoleController {

    private final RoleService roleService;

    /**
     * Get all roles
     */
    @GetMapping
    public ResponseEntity<List<RoleDTO>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    /**
     * Get role by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<RoleDTO> getRoleById(@PathVariable UUID id) {
        return ResponseEntity.ok(roleService.getRoleById(id));
    }

    /**
     * Get role by code
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<RoleDTO> getRoleByCode(@PathVariable String code) {
        return ResponseEntity.ok(roleService.getRoleByCode(code));
    }

    /**
     * Create new role
     */
    @PostMapping
    public ResponseEntity<RoleDTO> createRole(@Valid @RequestBody RoleDTO dto) {
        return ResponseEntity.ok(roleService.createRole(dto));
    }

    /**
     * Update role
     */
    @PutMapping("/{id}")
    public ResponseEntity<RoleDTO> updateRole(@PathVariable UUID id, @Valid @RequestBody RoleDTO dto) {
        return ResponseEntity.ok(roleService.updateRole(id, dto));
    }

    /**
     * Delete role
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteRole(@PathVariable UUID id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Role deleted successfully"));
    }

    /**
     * Add permission to role
     */
    @PostMapping("/{id}/permissions/{permissionCode}")
    public ResponseEntity<RoleDTO> addPermission(@PathVariable UUID id, @PathVariable String permissionCode) {
        return ResponseEntity.ok(roleService.addPermissionToRole(id, permissionCode));
    }

    /**
     * Remove permission from role
     */
    @DeleteMapping("/{id}/permissions/{permissionCode}")
    public ResponseEntity<RoleDTO> removePermission(@PathVariable UUID id, @PathVariable String permissionCode) {
        return ResponseEntity.ok(roleService.removePermissionFromRole(id, permissionCode));
    }
}
