import { inject } from '@angular/core';
import { Router, CanActivateFn, UrlTree } from '@angular/router';
import { TokenService } from '../services';

/**
 * Logged-In Guard - Prevents authenticated users from accessing auth pages (login/register)
 * Redirects logged-in users to their appropriate dashboard based on role
 *
 * Use case: Prevents users from seeing login form when clicking browser back button
 */
export const loggedInGuard: CanActivateFn = (route, state): boolean | UrlTree => {
  const tokenService = inject(TokenService);
  const router = inject(Router);

  // Check if user is already authenticated
  if (tokenService.isAuthenticated()) {
    // User is logged in, redirect to appropriate dashboard based on role
    const userRole = tokenService.getRole();

    console.log('User already authenticated, redirecting to dashboard...', userRole);

    // Return UrlTree for proper navigation instead of calling navigate() directly
    switch (userRole) {
      case 'ADMIN':
        return router.parseUrl('/admin-dashboard/dashboard');
      case 'CUSTOMER':
        return router.parseUrl('/customer/dashboard');
      case 'SURVEYOR':
        return router.parseUrl('/surveyor/dashboard');
      case 'UNDERWRITER':
        return router.parseUrl('/underwriter-dashboard/dashboard');
      case 'SIU_INVESTIGATOR':
        return router.parseUrl('/siu-dashboard/dashboard');
      default:
        // If role is not recognized, redirect to home
        console.warn('Unknown role:', userRole, '- redirecting to home');
        return router.parseUrl('/');
    }
  }

  // User is not authenticated, allow access to login/register
  return true;
};
