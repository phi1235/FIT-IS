import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { KeycloakService } from '../services/keycloak.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const keycloakService = inject(KeycloakService);

  // Skip interceptor for Keycloak endpoints
  if (req.url.includes('keycloak') || req.url.includes('auth')) {
    return next(req);
  }

  // Get token and add to request
  const token = keycloakService.getToken();
  
  if (token) {
    const clonedRequest = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    return next(clonedRequest);
  }

  return next(req);
};
