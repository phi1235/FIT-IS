import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { KeycloakService } from '../services/keycloak.service';

/**
 * Guard để bảo vệ Portal (Dashboard + Tickets)
 * Cho phép các role: admin, maker, checker
 */
export const portalGuard: CanActivateFn = (route, state) => {
    const keycloakService = inject(KeycloakService);
    const router = inject(Router);

    if (!keycloakService.isAuthenticated()) {
        keycloakService.login({
            redirectUri: window.location.origin + state.url
        });
        return false;
    }

    // Cho phép tất cả user đã đăng nhập vào portal (để gửi ticket)
    if (keycloakService.isAuthenticated()) {
        return true;
    } else {
        // Không có quyền, đẩy về home
        router.navigate(['/home']);
        return false;
    }
};
