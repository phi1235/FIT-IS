import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import Keycloak, { KeycloakLoginOptions } from 'keycloak-js';
import { BehaviorSubject, Observable } from 'rxjs';

export interface KeycloakConfig {
  url: string;
  realm: string;
  clientId: string;
}

@Injectable({
  providedIn: 'root'
})
export class KeycloakService {
  private keycloak: Keycloak | undefined;
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  public isAuthenticated$: Observable<boolean> = this.isAuthenticatedSubject.asObservable();

  // Keycloak configuration - Update these values according to your Keycloak setup
  private keycloakConfig: KeycloakConfig = {
    url: 'http://localhost:8080', // Keycloak server URL
    realm: 'phi-realm', // Your realm name
    clientId: 'angular-app' // Your client ID
  };

  constructor(private router: Router) {
    // Moved to APP_INITIALIZER
  }

  /**
   * Redirect user after successful login
   * All users go to home, admin can navigate to admin panel manually
   */
  private redirectAfterLogin(): void {
    // Navigate to home for all users
    const currentPath = window.location.pathname;
    if (currentPath === '/' || currentPath === '') {
      this.router.navigateByUrl('/home');
    }
  }

  /**
   * Initialize Keycloak
   */
  async initKeycloak(): Promise<boolean> {
    try {
      this.keycloak = new Keycloak({
        url: this.keycloakConfig.url,
        realm: this.keycloakConfig.realm,
        clientId: this.keycloakConfig.clientId
      });

      // Initialize without forcing login - just check if already authenticated
      const authenticated = await this.keycloak.init({
        pkceMethod: 'S256',
        checkLoginIframe: false
      });

      this.isAuthenticatedSubject.next(authenticated);

      if (authenticated) {
        console.log('User is authenticated via Keycloak SSO');
        this.setupTokenRefresh();
        this.redirectAfterLogin();
      } else {
        console.log('User not authenticated - showing app normally');
      }

      // Listen to authentication events
      this.keycloak.onAuthSuccess = () => {
        console.log('Authentication successful');
        this.isAuthenticatedSubject.next(true);
        this.setupTokenRefresh();
        this.redirectAfterLogin();
      };

      this.keycloak.onAuthError = () => {
        console.log('Authentication error');
        this.isAuthenticatedSubject.next(false);
      };

      this.keycloak.onAuthLogout = () => {
        console.log('User logged out');
        this.isAuthenticatedSubject.next(false);
      };

      this.keycloak.onTokenExpired = () => {
        console.log('Token expired');
        this.updateToken().then(refreshed => {
          if (!refreshed) {
            this.isAuthenticatedSubject.next(false);
          }
        });
      };

      return authenticated;
    } catch (error) {
      console.warn('Keycloak initialization failed - app will work without SSO', error);
      this.isAuthenticatedSubject.next(false);
      return false;
    }
  }

  /**
   * Login user
   */
  login(options?: KeycloakLoginOptions): void {
    if (this.keycloak) {
      this.keycloak.login(options);
    }
  }

  /**
   * Logout user
   */
  logout(): void {
    if (this.keycloak) {
      this.keycloak.logout();
    }
  }

  /**
   * Get access token
   */
  getToken(): string | undefined {
    return this.keycloak?.token;
  }

  /**
   * Get refresh token
   */
  getRefreshToken(): string | undefined {
    return this.keycloak?.refreshToken;
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    return this.keycloak?.authenticated ?? false;
  }

  /**
   * Get user profile
   */
  async getUserProfile(): Promise<any> {
    if (this.keycloak?.authenticated) {
      try {
        return await this.keycloak.loadUserProfile();
      } catch (error) {
        console.error('Failed to load user profile', error);
        return null;
      }
    }
    return null;
  }

  /**
   * Get user roles
   */
  getUserRoles(): string[] {
    if (this.keycloak?.tokenParsed) {
      const token = this.keycloak.tokenParsed as any;
      return token.realm_access?.roles || [];
    }
    return [];
  }

  /**
   * Check if user has role
   */
  hasRole(role: string): boolean {
    return this.getUserRoles().includes(role);
  }

  /**
   * Get username from token
   */
  getUsername(): string {
    if (this.keycloak?.tokenParsed) {
      const token = this.keycloak.tokenParsed as any;
      return token.preferred_username || token.sub || '';
    }
    return '';
  }

  /**
   * Get parsed user info from token
   */
  getUserInfo(): any {
    if (this.keycloak?.tokenParsed) {
      const token = this.keycloak.tokenParsed as any;
      return {
        id: token.userId || token.sub,
        username: token.preferred_username || token.sub,
        email: token.email,
        roles: this.getUserRoles()
      };
    }
    return null;
  }

  /**
   * Update token
   */
  async updateToken(minValidity: number = 5): Promise<boolean> {
    if (this.keycloak) {
      try {
        return await this.keycloak.updateToken(minValidity);
      } catch (error) {
        console.error('Failed to update token', error);
        return false;
      }
    }
    return false;
  }

  /**
   * Setup automatic token refresh
   */
  private setupTokenRefresh(): void {
    if (this.keycloak) {
      setInterval(() => {
        this.updateToken(30).then((refreshed) => {
          if (refreshed) {
            console.log('Token refreshed');
          }
        });
      }, 60000); // Check every minute
    }
  }

  /**
   * Register user
   */
  register(): void {
    if (this.keycloak) {
      this.keycloak.register();
    }
  }

  /**
   * Get account management URL
   */
  accountManagement(): void {
    if (this.keycloak) {
      this.keycloak.accountManagement();
    }
  }
}
