import { Component, OnInit } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ProfileService, MeDTO, UpdateMeRequest } from '../services/profile.service';
import { ToastService } from '../services/toast.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit {
  me: MeDTO | null = null;
  loading = true;
  editMode = false;
  saving = false;

  editForm: UpdateMeRequest = { firstName: '', lastName: '', email: '' };

  permissionGroups: { label: string; prefix: string; items: string[] }[] = [];

  constructor(
    private profileService: ProfileService,
    private authService: AuthService,
    private toastService: ToastService,
    private router: Router,
    private location: Location
  ) {}

  ngOnInit(): void {
    this.profileService.getMe().subscribe({
      next: (data) => {
        this.me = data;
        this.loading = false;
        this.buildPermissionGroups();
      },
      error: () => {
        // Fall back to JWT info
        const ui = this.authService.userInfo;
        if (ui) {
          this.me = {
            id: '',
            username: ui.username,
            email: ui.email || '',
            firstName: ui.firstName || '',
            lastName: ui.lastName || '',
            enabled: true,
            createdAt: '',
            roles: ui.roles,
            department: ui.department || '',
            position: ui.position || ''
          };
        }
        this.loading = false;
        this.buildPermissionGroups();
      }
    });
  }

  private buildPermissionGroups(): void {
    const perms = this.authService.userInfo?.permissions || [];
    const modules = [
      { label: 'Ticket', prefix: 'TICKET' },
      { label: 'User', prefix: 'USER' },
      { label: 'Role', prefix: 'ROLE' },
      { label: 'Email Template', prefix: 'EMAIL_TEMPLATE' },
      { label: 'Audit', prefix: 'AUDIT' },
      { label: 'Workflow', prefix: 'WORKFLOW' }
    ];
    this.permissionGroups = modules
      .map(m => ({ label: m.label, prefix: m.prefix, items: perms.filter(p => p.startsWith(m.prefix)) }))
      .filter(g => g.items.length > 0);
  }

  getInitials(): string {
    if (this.me?.firstName && this.me?.lastName) {
      return `${this.me.firstName[0]}${this.me.lastName[0]}`.toUpperCase();
    }
    return (this.me?.username?.[0] || '?').toUpperCase();
  }

  getDisplayName(): string {
    if (this.me?.firstName && this.me?.lastName) {
      return `${this.me.firstName} ${this.me.lastName}`;
    }
    return this.me?.username || '';
  }

  startEdit(): void {
    if (!this.me) return;
    this.editForm = {
      firstName: this.me.firstName,
      lastName: this.me.lastName,
      email: this.me.email
    };
    this.editMode = true;
  }

  cancelEdit(): void {
    this.editMode = false;
  }

  saveProfile(): void {
    if (this.saving) return;
    this.saving = true;
    this.profileService.updateMe(this.editForm).subscribe({
      next: (updated) => {
        this.me = updated;
        this.editMode = false;
        this.saving = false;
        this.authService.updateUserInfo({
          firstName: updated.firstName,
          lastName: updated.lastName,
          email: updated.email
        });
        this.toastService.success('Cập nhật thông tin thành công');
      },
      error: (err) => {
        this.saving = false;
        const msg = err.error?.message || 'Cập nhật thất bại';
        this.toastService.error(msg);
      }
    });
  }

  goBack(): void {
    this.location.back();
  }
}
