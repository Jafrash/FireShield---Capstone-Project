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

    console.log('User already authenticated, redirecting to dashboard...');

    switch (userRole) {
      case 'ADMIN':
        return router.createUrlTree(['/admin']);
      case 'CUSTOMER':
        return router.createUrlTree(['/customer']);
      case 'SURVEYOR':
        return router.createUrlTree(['/surveyor']);
      case 'UNDERWRITER':
        return router.createUrlTree(['/underwriter']);
      case 'SIU_INVESTIGATOR':
        return router.createUrlTree(['/siu-investigator']);
      default:
        // If role is not recognized or null, redirect to login for re-authentication
        return router.createUrlTree(['/auth/login']);
    }
  }

  // User is not authenticated, allow access to login/register
  return true;
};
