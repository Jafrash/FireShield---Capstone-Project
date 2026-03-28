import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { TokenService } from '../services';

/**
 * Auth Interceptor - Attaches JWT token to outgoing HTTP requests.
 * Proactively checks token expiry before sending.
 * Handles 401 errors ONLY when the user was authenticated — unauthenticated
 * requests that return 401 (e.g. public pages) are allowed to fail silently.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  const router = inject(Router);

  // Endpoints that never need Authorization headers
  const skipAuthEndpoints = ['/auth/login', '/auth/register'];
  const shouldSkipAuth = skipAuthEndpoints.some(endpoint => req.url.includes(endpoint));

  // Track whether this request was sent WITH a valid token
  // so we know if a 401 response means "your session expired"
  // vs "this endpoint requires auth you don't have"
  let requestWasAuthenticated = false;
  let clonedRequest = req;

  if (!shouldSkipAuth) {
    const token = tokenService.getToken();

    // ============================================
    // 🔴 CRITICAL FIX FOR FILE UPLOADS
    // ============================================
    // Do NOT set Content-Type header for FormData requests
    // Browser must set it automatically with multipart boundary
    
    if (req.body instanceof FormData) {
      console.log('📤 Interceptor: FormData request detected');
      console.log('   URL:', req.url);
      console.log('   ✅ Skipping Content-Type header (letting browser set it)');
      
      // Only add Authorization header for FormData
      if (token) {
        const cloned = req.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`
          }
        });
        
        console.log('   ✅ Authorization header added');
        return next(cloned).pipe(
          catchError((error: HttpErrorResponse) => {
            if (error.status === 401 && requestWasAuthenticated) {
              tokenService.clearAuth();
              router.navigate(['/auth/login'], {
                queryParams: { sessionExpired: 'true' }
              });
            }
            return throwError(() => error);
          })
        );
      }
      
      // No token, send request as-is
      return next(req).pipe(
          catchError((error: HttpErrorResponse) => {
            return throwError(() => error);
          })
      );
    }

    // For all non-FormData requests
    // For all non-FormData requests
    if (token) {
      if (tokenService.isTokenExpired()) {
        tokenService.clearAuth();
        router.navigate(['/auth/login'], {
          queryParams: { sessionExpired: 'true' }
        });
        return throwError(() => new Error('Token expired'));
      }

      const headers: any = {
        Authorization: `Bearer ${token}`
      };

      // Only add Content-Type for requests that might have a body
      if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(req.method)) {
        headers['Content-Type'] = 'application/json';
      }

      clonedRequest = req.clone({ setHeaders: headers });
      requestWasAuthenticated = true;
    }
  }

  return next(clonedRequest).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && requestWasAuthenticated) {
        tokenService.clearAuth();
        router.navigate(['/auth/login'], {
          queryParams: { sessionExpired: 'true' }
        });
      }
      return throwError(() => error);
    })
  );
};
