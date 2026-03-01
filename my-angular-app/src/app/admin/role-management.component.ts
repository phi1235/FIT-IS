import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RolePermissionService, Role, Permission } from '../services/role-permission.service';

@Component({
  selector: 'app-role-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './role-management.component.html',
  styleUrl: './role-management.component.css'
})
export class RoleManagementComponent implements OnInit {
  roles: Role[] = [];
  allPermissions: Permission[] = [];
  groupedPermissions: { [key: string]: Permission[] } = {};
  selectedRole: Role | null = null;
  
  isEditing = false;
  isSaving = false;
  isSyncing = false;
  currentTab: 'basic' | 'permissions' = 'permissions';
  
  // Matrix Columns
  readonly ACTION_COLUMNS = [
    { label: 'Xem', key: 'VIEW' },
    { label: 'Tạo mới', key: 'CREATE' },
    { label: 'Chỉnh sửa', key: 'UPDATE' },
    { label: 'Xóa', key: 'DELETE' },
    { label: 'Phê duyệt', key: 'APPROVE' },
    { label: 'Từ chối', key: 'REJECT' },
    { label: 'Xuất dữ liệu', key: 'EXPORT' }
  ];

  // Module Name Mapping
  readonly MODULE_DISPLAY_NAMES: { [key: string]: string } = {
    'AUTH': 'Quản lý Phân quyền',
    'USER': 'Quản lý Người dùng',
    'TICKET': 'Hệ thống Ticket',
    'REPORT': 'Báo cáo thống kê',
    'EMAIL': 'Mẫu Email',
    'SYSTEM': 'Cơ chế Hệ thống'
  };

  matrixData: any[] = [];
  
  // Form data
  roleForm: Partial<Role> = {
    name: '',
    code: '',
    description: ''
  };

  constructor(private roleService: RolePermissionService) {}

  ngOnInit() {
    this.loadRoles();
    this.loadPermissions();
  }

  loadRoles() {
    this.roleService.getRoles().subscribe(roles => {
      // Filter out admin role from editing
      this.roles = roles.filter(r => r.code.toLowerCase() !== 'admin');
      if (this.roles.length > 0 && !this.selectedRole) {
        this.selectRole(this.roles[0]);
      }
    });
  }

  loadPermissions() {
    this.roleService.getAllPermissions().subscribe(permissions => {
      this.allPermissions = permissions;
      this.groupPermissions();
    });
  }

  groupPermissions() {
    this.groupedPermissions = {};
    const modules: Set<string> = new Set();
    
    this.allPermissions.forEach(p => {
      // Normalize module to uppercase to merge duplicates (e.g., 'ticket' and 'TICKET')
      const moduleName = (p.module || 'OTHER').toUpperCase();
      modules.add(moduleName);
      
      if (!this.groupedPermissions[moduleName]) {
        this.groupedPermissions[moduleName] = [];
      }
      this.groupedPermissions[moduleName].push(p);
    });

    this.prepareMatrixData(Array.from(modules).sort());
  }

  private prepareMatrixData(modules: string[]) {
    this.matrixData = modules.map(moduleName => {
      const modulePermissions = this.groupedPermissions[moduleName] || [];
      const row: any = { module: moduleName };
      
      this.ACTION_COLUMNS.forEach(col => {
        row[col.key] = modulePermissions.find(p => this.isMatch(p.code, col.key));
      });
      
      // If none matched the standard columns, we could handle 'Other' but let's stick to standard for now
      return row;
    });
  }

  private isMatch(permCode: string, actionKey: string): boolean {
    const code = permCode.toUpperCase();
    switch (actionKey) {
      case 'VIEW': return code.includes('VIEW') || code.includes('LIST') || code.endsWith(':VIEW') || code.includes('ACCESS');
      case 'CREATE': return code.includes('CREATE') || code.includes('ADD') || code.endsWith(':CREATE');
      case 'UPDATE': return code.includes('MANAGE') || code.includes('UPDATE') || code.includes('EDIT') || code.includes('ASSIGN') || code.endsWith(':EDIT') || code.endsWith(':UPDATE');
      case 'DELETE': return code.includes('DELETE') || code.endsWith(':DELETE');
      case 'APPROVE': return code.includes('APPROVE') || code.includes('SUBMIT') || code.endsWith(':APPROVE');
      case 'REJECT': return code.includes('REJECT') || code.endsWith(':REJECT');
      case 'EXPORT': return code.includes('EXPORT') || code.endsWith(':EXPORT');
      default: return false;
    }
  }

  getModuleLabel(module: string): string {
    return this.MODULE_DISPLAY_NAMES[module.toUpperCase()] || module;
  }

  setTab(tab: 'basic' | 'permissions') {
    this.currentTab = tab;
  }

  selectRole(role: Role) {
    this.selectedRole = role;
    // Only copy basic info to form, permissions are handled separately
    this.roleForm = { 
      id: role.id,
      name: role.name,
      code: role.code,
      description: role.description,
      isSystem: role.isSystem
    };
    this.isEditing = false;
  }

  createNewRole() {
    this.selectedRole = null;
    this.roleForm = {
      name: '',
      code: '',
      description: '',
      isSystem: false
    };
    this.isEditing = true;
  }

  togglePermission(permission: Permission) {
    if (!this.selectedRole) return;

    const hasPermission = this.selectedRole.permissions.some(p => p.code === permission.code);
    
    if (hasPermission) {
      this.roleService.removePermissionFromRole(this.selectedRole.id, permission.code).subscribe(updatedRole => {
        this.selectedRole = updatedRole;
        this.updateRoleInList(updatedRole);
      });
    } else {
      this.roleService.addPermissionToRole(this.selectedRole.id, permission.code).subscribe(updatedRole => {
        this.selectedRole = updatedRole;
        this.updateRoleInList(updatedRole);
      });
    }
  }

  hasPermission(permission: Permission): boolean {
    return this.selectedRole?.permissions?.some(p => p.code === permission.code) || false;
  }

  saveRole() {
    this.isSaving = true;
    console.log('Saving role:', this.roleForm);
    if (this.selectedRole) {
      // For updates, we only send basic info to avoid overwriting permissions
      const updateData = {
        name: this.roleForm.name,
        code: this.roleForm.code,
        description: this.roleForm.description
      };
      this.roleService.updateRole(this.selectedRole.id, updateData).subscribe({
        next: (updated) => {
          console.log('Update success:', updated);
          this.selectedRole = updated;
          this.roleForm = { ...updated, permissions: undefined }; // Keep form in sync but without permissions
          this.updateRoleInList(updated);
          this.isSaving = false;
          this.isEditing = false;
          alert('Cập nhật role thành công!');
        },
        error: (err) => {
          console.error('Update failed:', err);
          this.isSaving = false;
          alert('Cập nhật role thất bại: ' + (err.error?.message || err.message));
        }
      });
    } else {
      this.roleService.createRole(this.roleForm).subscribe({
        next: (created) => {
          console.log('Create success:', created);
          this.roles.push(created);
          this.selectRole(created);
          this.isSaving = false;
          this.isEditing = false;
          alert('Tạo role mới thành công!');
        },
        error: (err) => {
          console.error('Create failed:', err);
          this.isSaving = false;
          alert('Tạo role mới thất bại: ' + (err.error?.message || err.message));
        }
      });
    }
  }

  deleteRole(role: Role) {
    if (confirm(`Are you sure you want to delete role ${role.name}?`)) {
      this.roleService.deleteRole(role.id).subscribe(() => {
        this.roles = this.roles.filter(r => r.id !== role.id);
        if (this.selectedRole?.id === role.id) {
          this.selectedRole = null;
        }
      });
    }
  }

  syncAdminRole() {
    this.isSyncing = true;
    this.roleService.syncAdminRole().subscribe({
      next: () => {
        this.isSyncing = false;
        alert('Đồng bộ Admin Role thành công!');
        this.loadRoles();
        this.loadPermissions();
      },
      error: (err) => {
        console.error('Sync failed:', err);
        this.isSyncing = false;
        alert('Đồng bộ thất bại: ' + (err.error?.message || err.message));
      }
    });
  }

  private updateRoleInList(updatedRole: Role) {
    const index = this.roles.findIndex(r => r.id === updatedRole.id);
    if (index !== -1) {
      this.roles[index] = updatedRole;
    }
  }
}
