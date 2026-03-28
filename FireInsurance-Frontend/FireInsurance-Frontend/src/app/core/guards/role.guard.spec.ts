import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { roleGuard } from './role.guard';
import { TokenService } from '../services';

describe('roleGuard', () => {
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

  const getRoute = (roles: string[]) => ({ data: { roles } } as any as ActivatedRouteSnapshot);
  const getState = (url: string) => ({ url } as RouterStateSnapshot);

  it('should grant access if authenticated and role matches', () => {
    mockTokenService.isAuthenticated.and.returnValue(true);
    mockTokenService.getRole.and.returnValue('ADMIN');

    const result = TestBed.runInInjectionContext(() => roleGuard(getRoute(['ADMIN', 'SURVEYOR']), getState('/admin/dashboard')));
    expect(result).toBeTrue();
  });

  it('should deny access if authenticated but role does NOT match', () => {
    mockTokenService.isAuthenticated.and.returnValue(true);
    mockTokenService.getRole.and.returnValue('CUSTOMER');

    const result = TestBed.runInInjectionContext(() => roleGuard(getRoute(['ADMIN']), getState('/admin/dashboard')));
    expect(result).toBeFalse();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/auth/unauthorized']);
  });

  it('should redirect to login if unauthenticated', () => {
    mockTokenService.isAuthenticated.and.returnValue(false);

    const result = TestBed.runInInjectionContext(() => roleGuard(getRoute(['ADMIN']), getState('/admin/dashboard')));
    expect(result).toBeFalse();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/auth/login'], { queryParams: { returnUrl: '/admin/dashboard' } });
  });
});
