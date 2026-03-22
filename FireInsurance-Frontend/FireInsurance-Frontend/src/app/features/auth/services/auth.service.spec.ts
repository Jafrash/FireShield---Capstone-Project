import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';
import { TokenService } from '../../../core/services/token.service';
import { Router } from '@angular/router';
import { environment } from '../../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpTestingController: HttpTestingController;
  let mockTokenService: jasmine.SpyObj<TokenService>;
  let mockRouter: jasmine.SpyObj<Router>;

  const baseUrl = `${environment.apiUrl}/auth`;

  beforeEach(() => {
    mockTokenService = jasmine.createSpyObj('TokenService', ['saveToken', 'saveRole', 'saveUsername', 'saveEmail', 'clearAuth', 'getRole', 'isAuthenticated']);
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: TokenService, useValue: mockTokenService },
        { provide: Router, useValue: mockRouter }
      ]
    });
    service = TestBed.inject(AuthService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should login and persist data correctly', () => {
    const creds = { username: 'test', password: 'password' };
    const mockRes = { token: 'header.eyJhIjoiYiJ9.signature', role: 'ADMIN', firstName: 'John', lastName: 'Doe', email: 'john@doe.com' };

    service.login(creds).subscribe(res => {
      expect(res).toEqual(mockRes as any);
    });

    const req = httpTestingController.expectOne(`${baseUrl}/login`);
    expect(req.request.method).toBe('POST');
    req.flush(mockRes);

    expect(mockTokenService.saveToken).toHaveBeenCalledWith('header.eyJhIjoiYiJ9.signature');
    expect(mockTokenService.saveRole).toHaveBeenCalledWith('ADMIN');
    expect(mockTokenService.saveUsername).toHaveBeenCalledWith('John Doe');
    expect(mockTokenService.saveEmail).toHaveBeenCalledWith('john@doe.com');
  });

  it('should register customer', () => {
    service.registerCustomer({} as any).subscribe();
    const req = httpTestingController.expectOne(`${baseUrl}/register/customer`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('should register surveyor', () => {
    service.registerSurveyor({} as any).subscribe();
    const req = httpTestingController.expectOne(`${baseUrl}/register/surveyor`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('should handle logic of logout', () => {
    service.logout();
    expect(mockTokenService.clearAuth).toHaveBeenCalled();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/auth/login']);
  });

  it('should redirect appropriately to ADMIN dash', () => {
    mockTokenService.getRole.and.returnValue('ADMIN');
    service.redirectToDashboard();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/admin']);
  });

  it('should redirect appropriately to CUSTOMER dash', () => {
    mockTokenService.getRole.and.returnValue('CUSTOMER');
    service.redirectToDashboard();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/customer']);
  });

  it('should navigate to login if unauthenticated or unknown role on redirect', () => {
    mockTokenService.getRole.and.returnValue(null as any);
    service.redirectToDashboard();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/auth/login']);
  });

  it('should check if isAuthenticated works via TokenService', () => {
    mockTokenService.isAuthenticated.and.returnValue(true);
    expect(service.isAuthenticated()).toBeTrue();
  });

  it('should handle google credential error if null is passed', () => {
    service.loginWithGoogle('').subscribe({
      error: (err) => {
        expect(err.message).toBe('Google credential was not provided.');
      }
    });
  });

  it('should dispatch password reset correctly', () => {
    service.resetCustomerPassword({ username: 'test', email: 'a@a', phoneNumber: '123', newPassword: 'abc' }).subscribe();
    const req = httpTestingController.expectOne(`${baseUrl}/forgot-password/customer`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });
});
