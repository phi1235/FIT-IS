import { Component, OnInit } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { KeycloakService } from './services/keycloak.service';
import { AuthService } from './services/auth.service';
import { ToastComponent } from './components/toast.component';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule, ToastComponent],
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
  isAuthPage = false;
  showUserMenu = false;

  constructor(
    private keycloakService: KeycloakService,
    private authService: AuthService,
    private router: Router
  ) { }

  toggleUserMenu(): void {
    this.showUserMenu = !this.showUserMenu;
  }

  closeUserMenu(): void {
    this.showUserMenu = false;
  }

  getDisplayName(): string {
    // Try from AuthService (custom login) first
    if (this.authService.isAuthenticated) {
      return this.authService.getDisplayName();
    }
    // Fallback to userProfile from Keycloak
    if (this.userProfile) {
      return this.userProfile.username || this.userProfile.preferred_username || '';
    }
    return this.keycloakService.getUsername() || 'User';
  }

  ngOnInit(): void {
    // Detect current route
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        this.isAdminRoute = event.urlAfterRedirects.startsWith('/admin');
        this.isAuthPage = event.urlAfterRedirects.startsWith('/login') || event.urlAfterRedirects.startsWith('/sign');
      });

    // Check custom auth (from AuthService)
    this.checkAuthState();

    // Subscribe to AuthService changes (custom login)
    this.authService.isAuthenticated$.subscribe(isAuth => {
      if (isAuth) {
        this.isAuthenticated = true;
        this.userProfile = this.authService.userInfo;
        this.isAdmin = this.authService.isAdmin;
        this.isInternalUser = true;
        console.log('Auth state updated:', {
          isAuthenticated: this.isAuthenticated,
          isAdmin: this.isAdmin,
          userProfile: this.userProfile,
          roles: this.userProfile?.roles
        });
      } else {
        // Check Keycloak if not custom authenticated
        this.checkKeycloakAuth();
      }
    });

    // Subscribe to Keycloak changes
    this.keycloakService.isAuthenticated$.subscribe(isAuth => {
      if (isAuth && !this.authService.isAuthenticated) {
        this.isAuthenticated = true;
        this.loadUserProfile();
      }
    });
  }

  private checkAuthState(): void {
    // Check custom auth first
    if (this.authService.isAuthenticated) {
      this.isAuthenticated = true;
      this.isAdmin = this.authService.isAdmin;
      this.isInternalUser = true;
      this.userProfile = this.authService.userInfo;
    } else {
      // Check Keycloak
      this.checkKeycloakAuth();
    }
  }

  private checkKeycloakAuth(): void {
    this.isAuthenticated = this.keycloakService.isAuthenticated();
    if (this.isAuthenticated) {
      this.loadUserProfile();
    } else {
      this.isAdmin = false;
      this.isInternalUser = false;
      this.userProfile = null;
    }
  }

  async loadUserProfile(): Promise<void> {
    try {
      this.userProfile = await this.keycloakService.getUserProfile();
      this.isAdmin = this.keycloakService.hasRole('admin');
      this.isInternalUser = true;
    } catch (error) {
      console.error('Failed to load user profile:', error);
      if (this.keycloakService.isAuthenticated()) {
        const token = this.keycloakService.getToken();
        if (token) {
          try {
            const tokenPayload = JSON.parse(atob(token.split('.')[1]));
            this.userProfile = {
              username: tokenPayload.preferred_username || tokenPayload.sub,
              email: tokenPayload.email
            };
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
    this.router.navigate(['/login']);
  }

  logout(): void {
    // Logout from both services
    if (this.authService.isAuthenticated) {
      this.authService.logout();
    }
    if (this.keycloakService.isAuthenticated()) {
      this.keycloakService.logout();
    }
    this.isAuthenticated = false;
    this.isAdmin = false;
    this.userProfile = null;
    this.router.navigate(['/home']);
  }
}
