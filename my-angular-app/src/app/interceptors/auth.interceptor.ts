import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { KeycloakService } from '../services/keycloak.service';
import { ErrorService } from '../services/error.service';

/**
 * HTTP Interceptor to:
 * 1. Auto-attach Authorization token to requests
 * 2. Handle errors centrally
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const keycloakService = inject(KeycloakService);
  const errorService = inject(ErrorService);

  // Skip interceptor for Keycloak endpoints
  if (req.url.includes('keycloak')) {
    return next(req);
  }

  // Get token from custom auth or Keycloak
  let token: string | null = null;

  if (authService.isAuthenticated) {
    token = authService.getToken();
  } else if (keycloakService.isAuthenticated()) {
    token = keycloakService.getToken() || null;
  }

  // Clone request and add Authorization header if token exists
  let authReq = req;
  if (token) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  // Handle request and catch errors
  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // Don't show toast for login endpoint errors (handled in component)
      if (!req.url.includes('/auth/login')) {
        errorService.handleError(error);
      }
      return throwError(() => error);
    })
  );
};
