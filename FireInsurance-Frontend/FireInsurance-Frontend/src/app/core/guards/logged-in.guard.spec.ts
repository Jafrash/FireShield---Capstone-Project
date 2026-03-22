import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { loggedInGuard } from './logged-in.guard';
import { TokenService } from '../services';

describe('loggedInGuard', () => {
  let mockTokenService: jasmine.SpyObj<TokenService>;
  let mockRouter: jasmine.SpyObj<Router>;

  beforeEach(() => {
    mockTokenService = jasmine.createSpyObj('TokenService', ['isAuthenticated', 'getRole']);
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        { provide: TokenService, useValue: mockTokenService },
        { provide: Router, useValue: mockRouter }
      ]
    });
  });

  const getRoute = () => ({} as ActivatedRouteSnapshot);
  const getState = (url: string) => ({ url } as RouterStateSnapshot);

  it('should allow access if user is NOT authenticated', () => {
    mockTokenService.isAuthenticated.and.returnValue(false);
    
    const result = TestBed.runInInjectionContext(() => loggedInGuard(getRoute(), getState('/auth/login')));
    expect(result).toBeTrue();
    expect(mockRouter.navigate).not.toHaveBeenCalled();
  });

  it('should redirect and block if user is authenticated (ADMIN)', () => {
    mockTokenService.isAuthenticated.and.returnValue(true);
    mockTokenService.getRole.and.returnValue('ADMIN');
    
    const result = TestBed.runInInjectionContext(() => loggedInGuard(getRoute(), getState('/auth/login')));
    expect(result).toBeFalse();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/admin/dashboard']);
  });

  it('should redirect and block if user is authenticated (CUSTOMER)', () => {
    mockTokenService.isAuthenticated.and.returnValue(true);
    mockTokenService.getRole.and.returnValue('CUSTOMER');
    
    const result = TestBed.runInInjectionContext(() => loggedInGuard(getRoute(), getState('/auth/login')));
    expect(result).toBeFalse();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/customer/dashboard']);
  });
});
