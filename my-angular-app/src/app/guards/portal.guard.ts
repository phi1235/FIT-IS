import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { KeycloakService } from '../services/keycloak.service';
import { AuthService } from '../services/auth.service';

/**
 * Guard để bảo vệ Portal (Dashboard + Tickets)
 * Cho phép các role: admin, maker, checker
 * Hỗ trợ cả Keycloak SSO và Custom login
 */
export const portalGuard: CanActivateFn = (route, state) => {
    const keycloakService = inject(KeycloakService);
    const authService = inject(AuthService);
    const router = inject(Router);

    // Check custom auth first (from AuthService)
    if (authService.isAuthenticated) {
        return true;
    }

    // Then check Keycloak
    if (keycloakService.isAuthenticated()) {
        return true;
    }

    // Not authenticated, redirect to login
    router.navigate(['/login']);
    return false;
};
