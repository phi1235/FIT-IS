import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Guard to protect routes based on required permissions.
 * Usage in app.routes.ts:
 * { path: 'path', component: Component, canActivate: [permissionGuard], data: { permission: 'PERM_CODE' } }
 */
export const permissionGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const requiredPermission = route.data['permission'] as string;

  if (!requiredPermission) {
    console.warn(`No permission data defined for route: ${state.url}`);
    return true;
  }

  if (authService.hasPermission(requiredPermission)) {
    return true;
  }

  // Not authorized, redirect to unauthorized page or home
  console.warn(`User does not have permission: ${requiredPermission}. Access denied to: ${state.url}`);
  router.navigate(['/home']);
  return false;
};
