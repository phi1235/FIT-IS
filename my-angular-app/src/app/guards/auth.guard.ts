import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { KeycloakService } from '../services/keycloak.service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const keycloakService = inject(KeycloakService);
  const router = inject(Router);

  if (authService.isAuthenticated || keycloakService.isAuthenticated()) {
    return true;
  }
  router.navigate(['/login']);
  return false;
};

/**
 * Guard to check if user has specific role
 */
export const roleGuard = (requiredRole: string): CanActivateFn => {
  return (route, state) => {
    const keycloakService = inject(KeycloakService);
    const router = inject(Router);

    if (keycloakService.isAuthenticated() && keycloakService.hasRole(requiredRole)) {
      return true;
    } else {
      router.navigate(['/home']);
      return false;
    }
  };
};
