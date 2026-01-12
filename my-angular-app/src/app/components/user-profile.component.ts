import { Component, ViewChild, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ChangePasswordModalComponent } from '../password-management/change-password-modal.component';

@Component({
  selector: 'app-user-profile',
  standalone: true,
  imports: [CommonModule, RouterLink, ChangePasswordModalComponent],
  template: `
    <div class="user-profile-wrapper">
      <button class="profile-button" (click)="toggleDropdown()" [class.active]="isDropdownOpen">
        <span class="avatar">{{ userInitial }}</span>
        <span class="username">{{ displayName }}</span>
        <span class="dropdown-icon">▼</span>
      </button>

      <div class="profile-dropdown" *ngIf="isDropdownOpen" (clickOutside)="closeDropdown()">
        <div class="dropdown-header">
          <div class="user-info">
            <div class="avatar-large">{{ userInitial }}</div>
            <div>
              <p class="name">{{ displayName }}</p>
              <p class="email">{{ userEmail }}</p>
            </div>
          </div>
        </div>

        <div class="dropdown-divider"></div>

        <div class="dropdown-menu">
          <button class="menu-item" (click)="openChangePasswordModal()">
            <span class="icon">🔐</span>
            <span>Change Password</span>
          </button>
          <button class="menu-item" *ngIf="isAdmin">
            <span class="icon">⚙️</span>
            <span routerLink="/admin">Admin Dashboard</span>
          </button>
          <a href="#" class="menu-item">
            <span class="icon">⚙️</span>
            <span>Settings</span>
          </a>
        </div>

        <div class="dropdown-divider"></div>

        <button class="menu-item logout" (click)="logout()">
          <span class="icon">🚪</span>
          <span>Logout</span>
        </button>
      </div>
    </div>

    <!-- Change Password Modal -->
    <app-change-password-modal 
      #changePasswordModal
      (close)="closeDropdown()"
    ></app-change-password-modal>
  `,
  styles: [`
    .user-profile-wrapper {
      position: relative;
    }

    .profile-button {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 8px 12px;
      background: none;
      border: 1px solid #e0e0e0;
      border-radius: 6px;
      cursor: pointer;
      transition: all 0.3s;
      font-size: 14px;
      color: #333;
    }

    .profile-button:hover {
      background-color: #f5f5f5;
      border-color: #999;
    }

    .profile-button.active {
      background-color: #f0f0f0;
      border-color: #667eea;
    }

    .avatar {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 600;
      font-size: 12px;
    }

    .username {
      font-weight: 500;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      max-width: 150px;
    }

    .dropdown-icon {
      font-size: 10px;
      color: #999;
      transition: transform 0.3s;
    }

    .profile-button.active .dropdown-icon {
      transform: rotate(180deg);
    }

    .profile-dropdown {
      position: absolute;
      top: calc(100% + 8px);
      right: 0;
      background: white;
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      min-width: 280px;
      z-index: 1000;
      animation: slideDown 0.2s ease-out;
    }

    @keyframes slideDown {
      from {
        opacity: 0;
        transform: translateY(-10px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    .dropdown-header {
      padding: 16px;
    }

    .user-info {
      display: flex;
      gap: 12px;
      align-items: flex-start;
    }

    .avatar-large {
      width: 48px;
      height: 48px;
      border-radius: 50%;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 600;
      font-size: 18px;
      flex-shrink: 0;
    }

    .name {
      margin: 0;
      font-weight: 600;
      color: #333;
      font-size: 14px;
    }

    .email {
      margin: 4px 0 0 0;
      color: #999;
      font-size: 12px;
    }

    .dropdown-divider {
      height: 1px;
      background-color: #f0f0f0;
    }

    .dropdown-menu {
      padding: 8px 0;
    }

    .menu-item {
      width: 100%;
      padding: 12px 16px;
      background: none;
      border: none;
      text-align: left;
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 12px;
      color: #333;
      font-size: 14px;
      transition: background-color 0.2s;
      text-decoration: none;
    }

    .menu-item:hover {
      background-color: #f9f9f9;
    }

    .menu-item.logout:hover {
      background-color: #fef2f2;
      color: #dc3545;
    }

    .icon {
      font-size: 16px;
      width: 20px;
    }
  `],
  host: {
    '(document:click)': 'onDocumentClick($event)'
  }
})
export class UserProfileComponent implements OnInit {
  @ViewChild('changePasswordModal') changePasswordModal!: ChangePasswordModalComponent;

  isDropdownOpen = false;
  displayName = '';
  userEmail = '';
  userInitial = '';
  isAdmin = false;

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.updateUserInfo();
    this.authService.userInfo$.subscribe(() => {
      this.updateUserInfo();
    });
  }

  private updateUserInfo(): void {
    const userInfo = this.authService.userInfo;
    if (userInfo) {
      this.displayName = this.authService.getDisplayName();
      this.userEmail = userInfo.email || '';
      this.userInitial = this.displayName.charAt(0).toUpperCase();
      this.isAdmin = this.authService.isAdmin;
    }
  }

  toggleDropdown(): void {
    this.isDropdownOpen = !this.isDropdownOpen;
  }

  closeDropdown(): void {
    this.isDropdownOpen = false;
  }

  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.user-profile-wrapper')) {
      this.closeDropdown();
    }
  }

  openChangePasswordModal(): void {
    this.changePasswordModal.openModal();
  }

  logout(): void {
    this.authService.logout();
  }
}
