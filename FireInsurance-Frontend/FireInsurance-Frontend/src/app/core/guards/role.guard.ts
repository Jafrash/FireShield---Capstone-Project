import { inject } from '@angular/core';
import { Router, CanActivateFn, ActivatedRouteSnapshot, UrlTree } from '@angular/router';
import { TokenService } from '../services';
import { UserRole } from '../models';

/**
 * Role Guard - Protects routes based on user role
 * Redirects to unauthorized page if user doesn't have required role
 */
export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot, state): boolean | UrlTree => {
  const tokenService = inject(TokenService);
  const router = inject(Router);

  const requiredRoles = route.data['roles'] as UserRole[];
  const userRole = tokenService.getRole();

  // Check if user is authenticated
  if (!tokenService.isAuthenticated()) {
    return router.createUrlTree(['/auth/login'], {
      queryParams: { returnUrl: state.url }
    });
  }

  // Check if user role matches required roles
  if (userRole && requiredRoles && requiredRoles.includes(userRole)) {
    return true;
  }

  // User is authenticated but doesn't have the required role - redirect to unauthorized
  console.warn('Access denied. Required roles:', requiredRoles, 'User role:', userRole);
  return router.createUrlTree(['/auth/unauthorized']);
};
