import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { KeycloakService } from '../services/keycloak.service';

/**
 * Guard để bảo vệ route admin
 * Chỉ cho phép user có role 'admin' truy cập
 */
export const adminGuard: CanActivateFn = (route, state) => {
  const keycloakService = inject(KeycloakService);
  const router = inject(Router);

  if (!keycloakService.isAuthenticated()) {
    // Chưa đăng nhập, redirect đến login
    keycloakService.login({
      redirectUri: window.location.origin + state.url
    });
    return false;
  }

  // Kiểm tra role admin
  if (keycloakService.hasRole('admin')) {
    return true;
  } else {
    // Không có quyền admin, redirect về home
    router.navigate(['/home']);
    return false;
  }
};

