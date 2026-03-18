import { Injectable } from '@angular/core';
import { UserRole } from '../models';

interface JwtPayload {
  sub: string;
  role: UserRole;
  authorities?: string[];
  email?: string;
  exp: number;
  iat: number;
}

@Injectable({
  providedIn: 'root'
})
export class TokenService {
  private readonly TOKEN_KEY = 'auth_token';
  private readonly ROLE_KEY = 'user_role';
  private readonly USERNAME_KEY = 'username';
  private readonly EMAIL_KEY = 'user_email';

  constructor() {}

  /**
   * Save authentication token to localStorage
   */
  saveToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  /**
   * Get authentication token from localStorage
   */
  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  /**
   * Check if token exists
   */
  hasToken(): boolean {
    return !!this.getToken();
  }

  /**
   * Save user role to localStorage
   */
  saveRole(role: UserRole): void {
    localStorage.setItem(this.ROLE_KEY, role);
  }

  /**
   * Get user role from localStorage
   */
  getRole(): UserRole | null {
    return localStorage.getItem(this.ROLE_KEY) as UserRole | null;
  }

  /**
   * Save username to localStorage
   */
  saveUsername(username: string): void {
    localStorage.setItem(this.USERNAME_KEY, username);
  }

  /**
   * Get username from localStorage
   */
  getUsername(): string | null {
    return localStorage.getItem(this.USERNAME_KEY);
  }

  /**
   * Save email to localStorage
   */
  saveEmail(email: string): void {
    localStorage.setItem(this.EMAIL_KEY, email);
  }

  /**
   * Get email from localStorage
   */
  getEmail(): string | null {
    return localStorage.getItem(this.EMAIL_KEY);
  }

  /**
   * Check if user has specific role
   */
  hasRole(role: UserRole): boolean {
    return this.getRole() === role;
  }

  /**
   * Clear all authentication data
   */
  clearAuth(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.ROLE_KEY);
    localStorage.removeItem(this.USERNAME_KEY);
    localStorage.removeItem(this.EMAIL_KEY);
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    return this.hasToken() && !!this.getRole() && !this.isTokenExpired();
  }

  /**
   * Decode JWT payload (base64url) without verifying signature
   */
  private decodePayload(): JwtPayload | null {
    const token = this.getToken();
    if (!token) return null;
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      return JSON.parse(window.atob(base64)) as JwtPayload;
    } catch {
      return null;
    }
  }

  /**
   * Check if the stored JWT token is expired
   */
  isTokenExpired(): boolean {
    const payload = this.decodePayload();
    if (!payload || !payload.exp) return true;
    // exp is in seconds; Date.now() is in milliseconds
    return Date.now() >= payload.exp * 1000;
  }

  /**
   * Get token expiry date for display
   */
  getTokenExpiryDate(): Date | null {
    const payload = this.decodePayload();
    if (!payload || !payload.exp) return null;
    return new Date(payload.exp * 1000);
  }
}
