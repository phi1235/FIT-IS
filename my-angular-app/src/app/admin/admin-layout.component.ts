import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterOutlet, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { KeycloakService } from '../services/keycloak.service';
import { AuthService } from '../services/auth.service';
import { PermissionDirective } from '../directives/permission.directive';
import { ChangePasswordModalComponent } from '../password-management/change-password-modal.component';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, PermissionDirective, ChangePasswordModalComponent],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.css'
})
export class AdminLayoutComponent implements OnInit {
  @ViewChild(ChangePasswordModalComponent) changePasswordModal!: ChangePasswordModalComponent;
  currentUser: any = null;
  isAuthenticated = false;
  isAdmin = false;
  showUserDropdown = false;

  breadcrumbs: { label: string; url: string; isLast: boolean }[] = [];

  private readonly LABELS: Record<string, string> = {
    'admin':           'Portal',
    'tickets':         'Tickets',
    'create':          'Tạo mới',
    'users':           'Quản lý người dùng',
    'password-reset':  'Đặt lại mật khẩu',
    'roles':           'Quản lý phân quyền',
    'email-templates': 'Email Templates',
    'new':             'Tạo mới',
    'audit-logs':      'Nhật ký kiểm toán',
    'notifications':   'Thông báo',
    'workflow':        'Workflows',
  };

  private buildBreadcrumbs(): void {
    const url = this.router.url.split('?')[0];
    const segments = url.split('/').filter(s => s);
    const crumbs: { label: string; url: string }[] = [];
    let currentUrl = '';
    for (const segment of segments) {
      currentUrl += '/' + segment;
      const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(segment);
      const label = isUuid ? 'Chi tiết' : (this.LABELS[segment] || segment);
      crumbs.push({ label, url: currentUrl });
    }
    this.breadcrumbs = crumbs.map((c, i) => ({ ...c, isLast: i === crumbs.length - 1 }));
  }

  toggleUserDropdown(): void {
    this.showUserDropdown = !this.showUserDropdown;
  }

  constructor(
    private keycloakService: KeycloakService,
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit() {
    this.buildBreadcrumbs();
    this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe(() => {
      this.buildBreadcrumbs();
    });

    // Check custom auth first
    if (this.authService.isAuthenticated) {
      this.isAuthenticated = true;
      this.currentUser = this.authService.userInfo;
      this.isAdmin = this.authService.isAdmin;
    } else if (this.keycloakService.isAuthenticated()) {
      this.isAuthenticated = true;
      this.loadKeycloakProfile();
    }

    // Subscribe to AuthService changes
    this.authService.isAuthenticated$.subscribe(isAuth => {
      if (isAuth) {
        this.isAuthenticated = true;
        this.currentUser = this.authService.userInfo;
        this.isAdmin = this.authService.isAdmin;
      }
    });

    // Subscribe to Keycloak changes
    this.keycloakService.isAuthenticated$.subscribe(isAuth => {
      if (isAuth && !this.authService.isAuthenticated) {
        this.isAuthenticated = true;
        this.loadKeycloakProfile();
      }
    });
  }

  async loadKeycloakProfile() {
    try {
      this.currentUser = await this.keycloakService.getUserProfile();
      this.isAdmin = this.keycloakService.hasRole('admin');
    } catch (error) {
      console.error('Failed to load user profile:', error);
      if (this.keycloakService.isAuthenticated()) {
        const token = this.keycloakService.getToken();
        if (token) {
          try {
            const tokenPayload = JSON.parse(atob(token.split('.')[1]));
            this.currentUser = {
              username: tokenPayload.preferred_username || tokenPayload.sub,
              email: tokenPayload.email
            };
          } catch (e) {
            console.error('Failed to parse token:', e);
          }
        }
      }
    }
  }

  getDisplayName(): string {
    if (this.currentUser) {
      return this.currentUser.username || this.currentUser.preferred_username || 'User';
    }
    return 'User';
  }

  isMaker(): boolean {
    return this.authService.hasRole('MAKER');
  }

  isChecker(): boolean {
    return this.authService.hasRole('CHECKER');
  }

  openChangePassword() {
    this.changePasswordModal.openModal();
  }

  logout() {
    if (this.authService.isAuthenticated) {
      this.authService.logout();
    } else if (this.keycloakService.isAuthenticated()) {
      this.keycloakService.logout();
    } else {
      this.router.navigate(['/home']);
    }
  }
}
