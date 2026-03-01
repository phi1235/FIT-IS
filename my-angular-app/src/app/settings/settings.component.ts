import { Component, OnInit } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { PasswordManagementService } from '../services/password-management.service';
import { ToastService } from '../services/toast.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.css'
})
export class SettingsComponent implements OnInit {
  userInfo: any = null;

  // Password change form
  passwordForm = { current: '', newPw: '', confirm: '' };
  passwordLoading = false;
  showCurrent = false;
  showNew = false;
  showConfirm = false;

  permissionGroups: { label: string; prefix: string; items: string[] }[] = [];

  constructor(
    private authService: AuthService,
    private passwordService: PasswordManagementService,
    private toastService: ToastService,
    private location: Location
  ) {}

  ngOnInit(): void {
    this.userInfo = this.authService.userInfo;
    this.buildPermissionGroups();
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

  get passwordMismatch(): boolean {
    return !!(this.passwordForm.newPw && this.passwordForm.confirm && this.passwordForm.newPw !== this.passwordForm.confirm);
  }

  get passwordTooShort(): boolean {
    return !!(this.passwordForm.newPw && this.passwordForm.newPw.length < 8);
  }

  get canSubmitPassword(): boolean {
    return !!(
      this.passwordForm.current &&
      this.passwordForm.newPw &&
      this.passwordForm.confirm &&
      !this.passwordMismatch &&
      !this.passwordTooShort
    );
  }

  changePassword(): void {
    if (!this.canSubmitPassword || this.passwordLoading) return;
    this.passwordLoading = true;
    this.passwordService.changePassword(
      this.passwordForm.current,
      this.passwordForm.newPw,
      this.passwordForm.confirm
    ).subscribe({
      next: (res) => {
        this.passwordLoading = false;
        this.toastService.success(res.message || 'Đổi mật khẩu thành công');
        this.passwordForm = { current: '', newPw: '', confirm: '' };
      },
      error: (err) => {
        this.passwordLoading = false;
        const msg = err.error?.message || 'Đổi mật khẩu thất bại';
        this.toastService.error(msg);
      }
    });
  }

  getInitials(): string {
    const ui = this.userInfo;
    if (ui?.firstName && ui?.lastName) {
      return `${ui.firstName[0]}${ui.lastName[0]}`.toUpperCase();
    }
    return (ui?.username?.[0] || '?').toUpperCase();
  }

  getDisplayName(): string {
    const ui = this.userInfo;
    if (ui?.firstName && ui?.lastName) {
      return `${ui.firstName} ${ui.lastName}`;
    }
    return ui?.username || '';
  }

  goBack(): void {
    this.location.back();
  }
}
