import { Component, OnInit } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { KeycloakService } from './services/keycloak.service';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  title = 'Angular Keycloak App';
  isAuthenticated = false;
  userProfile: any = null;
  isAdmin = false;
  isInternalUser = false;
  isAdminRoute = false;
  showUserMenu = false;

  constructor(
    private keycloakService: KeycloakService,
    private router: Router
  ) { }

  toggleUserMenu(): void {
    this.showUserMenu = !this.showUserMenu;
  }

  // Close dropdown when clicking outside
  closeUserMenu(): void {
    this.showUserMenu = false;
  }

  getDisplayName(): string {
    // Try from userProfile first
    if (this.userProfile) {
      return this.userProfile.username || this.userProfile.preferred_username || '';
    }
    // Fallback to keycloak service
    return this.keycloakService.getUsername() || 'User';
  }

  ngOnInit(): void {
    // Detect current route to hide navbar on admin pages
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        this.isAdminRoute = event.urlAfterRedirects.startsWith('/admin');
      });

    // Check initial authentication state
    this.isAuthenticated = this.keycloakService.isAuthenticated();
    if (this.isAuthenticated) {
      this.loadUserProfile();
    }

    // Subscribe to authentication state changes
    this.keycloakService.isAuthenticated$.subscribe(isAuth => {
      this.isAuthenticated = isAuth;
      if (isAuth) {
        this.loadUserProfile();
      } else {
        this.userProfile = null;
      }
    });
  }

  async loadUserProfile(): Promise<void> {
    try {
      this.userProfile = await this.keycloakService.getUserProfile();
      console.log('User profile loaded:', this.userProfile);
      // Check roles
      this.isAdmin = this.keycloakService.hasRole('admin');
      this.isInternalUser = true; // Bất kỳ ai đăng nhập đều có thể xem/tạo tickets
    } catch (error) {
      console.error('Failed to load user profile:', error);
      // Fallback: create minimal profile from token
      if (this.keycloakService.isAuthenticated()) {
        const token = this.keycloakService.getToken();
        if (token) {
          // Try to get username from token
          try {
            const tokenPayload = JSON.parse(atob(token.split('.')[1]));
            this.userProfile = {
              username: tokenPayload.preferred_username || tokenPayload.sub,
              email: tokenPayload.email
            };
            // Check admin role from token
            const roles = tokenPayload.realm_access?.roles || [];
            this.isAdmin = roles.includes('admin');
            this.isInternalUser = true;
          } catch (e) {
            console.error('Failed to parse token:', e);
          }
        }
      }
    }
  }

  login(): void {
    this.keycloakService.login({
      redirectUri: window.location.origin + '/home'
    });
  }

  logout(): void {
    this.keycloakService.logout();
  }
}
