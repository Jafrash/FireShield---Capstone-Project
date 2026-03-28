import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { authGuard } from './auth.guard';
import { TokenService } from '../services';

describe('authGuard', () => {
  let mockTokenService: jasmine.SpyObj<TokenService>;
  let mockRouter: jasmine.SpyObj<Router>;

  beforeEach(() => {
    mockTokenService = jasmine.createSpyObj('TokenService', ['isAuthenticated']);
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

  it('should allow access if user is authenticated', () => {
    mockTokenService.isAuthenticated.and.returnValue(true);
    
    const result = TestBed.runInInjectionContext(() => authGuard(getRoute(), getState('/test')));
    expect(result).toBeTrue();
    expect(mockRouter.navigate).not.toHaveBeenCalled();
  });

  it('should redirect if user is not authenticated', () => {
    mockTokenService.isAuthenticated.and.returnValue(false);
    
    const result = TestBed.runInInjectionContext(() => authGuard(getRoute(), getState('/protected')));
    expect(result).toBeFalse();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/auth/login'], { queryParams: { returnUrl: '/protected' } });
  });
});
