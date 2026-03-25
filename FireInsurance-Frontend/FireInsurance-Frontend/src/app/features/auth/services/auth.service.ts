import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, throwError } from 'rxjs';
import { 
  LoginRequest, 
  LoginResponse, 
  GoogleLoginRequest,
  ForgotPasswordRequest,
  ForgotPasswordResponse,
  RegisterCustomerRequest,
  RegisterSurveyorRequest,
  RegisterResponse,
  UserRole 
} from '../../../core/models';
import { TokenService } from '../../../core/services/token.service';
import { environment } from '../../../../environments/environment';

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
export class AuthService {
  private readonly apiUrl = `${environment.apiUrl}/auth`;
  private http = inject(HttpClient);
  private tokenService = inject(TokenService);
  private router = inject(Router);

  /**
   * Login user and store JWT token
   */
  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => {
        this.persistAuthentication(response);
      })
    );
  }

  /**
   * Exchange Google credential for application JWT
   */
  loginWithGoogle(credential: string): Observable<LoginResponse> {
    if (!credential) {
      return throwError(() => new Error('Google credential was not provided.'));
    }

    const request: GoogleLoginRequest = { credential };
    return this.http.post<LoginResponse>(`${this.apiUrl}/login/google`, request).pipe(
      tap(response => {
        this.persistAuthentication(response, 'CUSTOMER');
      })
    );
  }

  /**
   * Reset a customer password after verifying account details.
   */
  resetCustomerPassword(request: ForgotPasswordRequest): Observable<ForgotPasswordResponse> {
    return this.http.post<ForgotPasswordResponse>(`${this.apiUrl}/forgot-password/customer`, request);
  }

  /**
   * Persist authenticated user data from JWT response.
   */
  private persistAuthentication(response: LoginResponse, expectedRole?: UserRole): void {
    // Decode JWT token to get user info before persisting role checks.
    const decodedToken = this.decodeToken(response.token);
    const role = (response.role || decodedToken?.role || decodedToken?.authorities?.[0] || 'CUSTOMER') as UserRole;

    if (expectedRole && role !== expectedRole) {
      this.tokenService.clearAuth();
      throw new Error('Google login is available for customer accounts only.');
    }

    this.tokenService.saveToken(response.token);
    this.tokenService.saveRole(role);

    if (response.firstName && response.lastName) {
      this.tokenService.saveUsername(`${response.firstName} ${response.lastName}`);
    } else if (decodedToken?.sub) {
      this.tokenService.saveUsername(decodedToken.sub);
    }

    if (response.email) {
      this.tokenService.saveEmail(response.email);
    } else if (decodedToken?.email) {
      this.tokenService.saveEmail(decodedToken.email);
    }
  }

  /**
   * Decode JWT token to extract payload
   */
  private decodeToken(token: string): JwtPayload | null {
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(atob(base64).split('').map(c => {
        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
      }).join(''));
      return JSON.parse(jsonPayload) as JwtPayload;
    } catch (error) {
      console.error('Error decoding token:', error);
      return null;
    }
  }

  /**
   * Register a new customer
   */
  registerCustomer(data: RegisterCustomerRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.apiUrl}/register/customer`, data);
  }

  /**
   * Register a new surveyor
   */
  registerSurveyor(data: RegisterSurveyorRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.apiUrl}/register/surveyor`, data);
  }

  /**
   * Logout user and clear authentication data
   */
  logout(): void {
    this.tokenService.clearAuth();
    this.router.navigate(['/auth/login']);
  }

  /**
   * Redirect user to appropriate dashboard based on role
   */
  redirectToDashboard(): void {
    const role = this.tokenService.getRole();

    if (!role) {
      this.router.navigate(['/auth/login']);
      return;
    }

    switch (role) {
      case 'ADMIN':
        this.router.navigate(['/admin-dashboard']);
        break;
      case 'CUSTOMER':
        this.router.navigate(['/customer']);
        break;
      case 'SURVEYOR':
        this.router.navigate(['/surveyor']);
        break;
      case 'UNDERWRITER':
        this.router.navigate(['/underwriter-dashboard']);
        break;
      case 'SIU_INVESTIGATOR':
        this.router.navigate(['/siu-dashboard']);
        break;
      default:
        this.router.navigate(['/auth/login']);
    }
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    return this.tokenService.isAuthenticated();
  }

  /**
   * Get current user role
   */
  getCurrentRole(): UserRole | null {
    return this.tokenService.getRole();
  }
}
