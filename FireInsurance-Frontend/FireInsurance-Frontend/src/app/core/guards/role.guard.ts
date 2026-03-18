import { inject } from '@angular/core';
import { Router, CanActivateFn, ActivatedRouteSnapshot } from '@angular/router';
import { TokenService } from '../services';
import { UserRole } from '../models';

/**
 * Role Guard - Protects routes based on user role
 * Redirects to unauthorized page if user doesn't have required role
 */
export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot, state) => {
  const tokenService = inject(TokenService);
  const router = inject(Router);

  const requiredRoles = route.data['roles'] as UserRole[];
  const userRole = tokenService.getRole();

  // Check if user is authenticated
  if (!tokenService.isAuthenticated()) {
    router.navigate(['/auth/login'], {
      queryParams: { returnUrl: state.url }
    });
    return false;
  }

  // Check if user role matches required roles
  if (userRole && requiredRoles && requiredRoles.includes(userRole)) {
    return true;
  }

  // User is authenticated but doesn't have the required role - redirect to unauthorized
  router.navigate(['/auth/unauthorized']);
  return false;
};
