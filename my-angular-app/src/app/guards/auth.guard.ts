import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { KeycloakService } from '../services/keycloak.service';

export const authGuard: CanActivateFn = (route, state) => {
  const keycloakService = inject(KeycloakService);
  const router = inject(Router);

  if (keycloakService.isAuthenticated()) {
    return true;
  } else {
    // Redirect to login
    keycloakService.login();
    return false;
  }
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
