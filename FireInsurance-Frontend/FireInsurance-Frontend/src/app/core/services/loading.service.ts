import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class LoadingService {
  // Track number of active HTTP requests
  private activeRequests = signal<number>(0);
  
  // Public loading state - true when any request is active
  public isLoading = signal<boolean>(false);

  /**
   * Increment active request count
   */
  show(): void {
    this.activeRequests.update(count => count + 1);
    this.isLoading.set(true);
  }

  /**
   * Decrement active request count
   * Only hide loading when all requests complete
   */
  hide(): void {
    this.activeRequests.update(count => {
      const newCount = Math.max(0, count - 1);
      if (newCount === 0) {
        this.isLoading.set(false);
      }
      return newCount;
    });
  }

  /**
   * Force hide loading (useful for error scenarios)
   */
  forceHide(): void {
    this.activeRequests.set(0);
    this.isLoading.set(false);
  }

  /**
   * Get current active request count
   */
  getActiveRequestCount(): number {
    return this.activeRequests();
  }
}
