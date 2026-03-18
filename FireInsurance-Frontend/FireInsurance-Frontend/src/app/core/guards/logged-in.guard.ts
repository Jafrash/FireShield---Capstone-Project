import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { TokenService } from '../services';

/**
 * Logged-In Guard - Prevents authenticated users from accessing auth pages (login/register)
 * Redirects logged-in users to their appropriate dashboard based on role
 * 
 * Use case: Prevents users from seeing login form when clicking browser back button
 */
export const loggedInGuard: CanActivateFn = (route, state) => {
  const tokenService = inject(TokenService);
  const router = inject(Router);

  // Check if user is already authenticated
  if (tokenService.isAuthenticated()) {
    // User is logged in, redirect to appropriate dashboard based on role
    const userRole = tokenService.getRole();
    
    console.log('User already authenticated, redirecting to dashboard...');
    
    switch (userRole) {
      case 'ADMIN':
        router.navigate(['/admin/dashboard']);
        break;
      case 'CUSTOMER':
        router.navigate(['/customer/dashboard']);
        break;
      case 'SURVEYOR':
        router.navigate(['/surveyor/dashboard']);
        break;
      case 'UNDERWRITER':
        router.navigate(['/underwriter/dashboard']);
        break;
      default:
        // If role is not recognized, redirect to home
        router.navigate(['/']);
        break;
    }
    
    return false; // Block access to login/register pages
  }

  // User is not authenticated, allow access to login/register
  return true;
};
