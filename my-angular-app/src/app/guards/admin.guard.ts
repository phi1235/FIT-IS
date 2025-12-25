import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { KeycloakService } from '../services/keycloak.service';
import { AuthService } from '../services/auth.service';

/**
 * Guard để bảo vệ route admin
 * Chỉ cho phép user có role 'admin' truy cập
 * Hỗ trợ cả Keycloak SSO và Custom login
 */
export const adminGuard: CanActivateFn = (route, state) => {
  const keycloakService = inject(KeycloakService);
  const authService = inject(AuthService);
  const router = inject(Router);

  // Check custom auth first (from AuthService)
  if (authService.isAuthenticated) {
    if (authService.isAdmin) {
      return true;
    } else {
      router.navigate(['/home']);
      return false;
    }
  }

  // Then check Keycloak
  if (keycloakService.isAuthenticated()) {
    if (keycloakService.hasRole('admin')) {
      return true;
    } else {
      router.navigate(['/home']);
      return false;
    }
  }

  // Not authenticated, redirect to login
  router.navigate(['/login']);
  return false;
};
