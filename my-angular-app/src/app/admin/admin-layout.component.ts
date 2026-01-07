import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { KeycloakService } from '../services/keycloak.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.css'
})
export class AdminLayoutComponent implements OnInit {
  currentUser: any = null;
  isAuthenticated = false;
  isAdmin = false;

  constructor(
    private keycloakService: KeycloakService,
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit() {
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
