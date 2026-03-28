import { inject } from '@angular/core';
import { Router, CanActivateFn, UrlTree } from '@angular/router';
import { TokenService } from '../services';

/**
 * Auth Guard - Protects routes that require authentication
 * Redirects to login if user is not authenticated
 */
export const authGuard: CanActivateFn = (route, state): boolean | UrlTree => {
  const tokenService = inject(TokenService);
  const router = inject(Router);

  if (tokenService.isAuthenticated()) {
    return true;
  }

  // Store attempted URL for redirecting after login
  return router.createUrlTree(['/auth/login'], {
    queryParams: { returnUrl: state.url }
  });
};
