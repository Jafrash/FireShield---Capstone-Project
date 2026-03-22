import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors, HttpErrorResponse } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';
import { TokenService } from '../services';
import { Router } from '@angular/router';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpTestingController: HttpTestingController;
  let mockTokenService: jasmine.SpyObj<TokenService>;
  let mockRouter: jasmine.SpyObj<Router>;

  beforeEach(() => {
    mockTokenService = jasmine.createSpyObj('TokenService', ['getToken', 'isTokenExpired', 'clearAuth']);
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: TokenService, useValue: mockTokenService },
        { provide: Router, useValue: mockRouter }
      ]
    });

    httpClient = TestBed.inject(HttpClient);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should add Authorization header for regular requests', () => {
    mockTokenService.getToken.and.returnValue('test-token');
    mockTokenService.isTokenExpired.and.returnValue(false);

    httpClient.get('/api/data').subscribe();

    const req = httpTestingController.expectOne('/api/data');
    expect(req.request.headers.get('Authorization')).toBe('Bearer test-token');
    expect(req.request.headers.get('Content-Type')).toBe('application/json');
  });

  it('should NOT add Content-Type header for FormData requests', () => {
    mockTokenService.getToken.and.returnValue('test-token');
    mockTokenService.isTokenExpired.and.returnValue(false);

    const formData = new FormData();
    formData.append('file', new Blob([''], { type: 'text/plain' }));

    httpClient.post('/api/upload', formData).subscribe();

    const req = httpTestingController.expectOne('/api/upload');
    expect(req.request.headers.get('Authorization')).toBe('Bearer test-token');
    expect(req.request.headers.has('Content-Type')).toBeFalse();
  });

  it('should skip auth for login and register endpoints', () => {
    httpClient.post('/auth/login', {}).subscribe();
    const req1 = httpTestingController.expectOne('/auth/login');
    expect(req1.request.headers.has('Authorization')).toBeFalse();

    httpClient.post('/auth/register', {}).subscribe();
    const req2 = httpTestingController.expectOne('/auth/register');
    expect(req2.request.headers.has('Authorization')).toBeFalse();
  });

  it('should redirect to login if token is expired', () => {
    mockTokenService.getToken.and.returnValue('expired-token');
    mockTokenService.isTokenExpired.and.returnValue(true);

    httpClient.get('/api/data').subscribe({
      error: (err) => expect(err.message).toBe('Token expired')
    });

    expect(mockTokenService.clearAuth).toHaveBeenCalled();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/auth/login'], jasmine.any(Object));
    httpTestingController.expectNone('/api/data');
  });

  it('should handle 401 error and redirect to login if request was authenticated', () => {
    mockTokenService.getToken.and.returnValue('valid-token');
    mockTokenService.isTokenExpired.and.returnValue(false);

    httpClient.get('/api/data').subscribe({
      error: (err) => expect(err.status).toBe(401)
    });

    const req = httpTestingController.expectOne('/api/data');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(mockTokenService.clearAuth).toHaveBeenCalled();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/auth/login'], jasmine.any(Object));
  });
});
