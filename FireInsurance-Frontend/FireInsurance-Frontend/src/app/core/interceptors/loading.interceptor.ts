import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';
import { LoadingService } from '../services/loading.service';

/**
 * Loading Interceptor - Automatically shows/hides loading indicator
 * for all HTTP requests except those explicitly skipped
 */
export const loadingInterceptor: HttpInterceptorFn = (req, next) => {
  const loadingService = inject(LoadingService);

  // Endpoints that should NOT trigger loading indicator
  const skipLoadingEndpoints = [
    '/api/auth/refresh',  // Background token refresh
    '/api/notifications', // Polling endpoints
    '/api/heartbeat'      // Health checks
  ];

  // Check if this request should skip loading indicator
  const shouldSkip = skipLoadingEndpoints.some(endpoint => 
    req.url.includes(endpoint)
  );

  // Also check for custom header to skip loading
  const hasSkipHeader = req.headers.has('X-Skip-Loading');

  if (shouldSkip || hasSkipHeader) {
    // Remove custom header before sending to backend
    const modifiedReq = req.clone({
      headers: req.headers.delete('X-Skip-Loading')
    });
    return next(modifiedReq);
  }

  // Show loading indicator
  loadingService.show();

  // Continue with the request and hide loading when complete
  return next(req).pipe(
    finalize(() => {
      // Hide loading indicator after request completes (success or error)
      loadingService.hide();
    })
  );
};
