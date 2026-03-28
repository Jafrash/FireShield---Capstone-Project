import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ForgotPasswordComponent } from './forgot-password';
import { AuthService } from '../../services/auth.service';
import { provideRouter, ActivatedRoute, Router } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { of, throwError, delay } from 'rxjs';

describe('ForgotPasswordComponent', () => {
  let component: ForgotPasswordComponent;
  let fixture: ComponentFixture<ForgotPasswordComponent>;
  let mockAuthService: jasmine.SpyObj<AuthService>;
  let mockRouter: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    mockAuthService = jasmine.createSpyObj('AuthService', ['resetCustomerPassword']);
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [ForgotPasswordComponent, ReactiveFormsModule],
      providers: [
        { provide: AuthService, useValue: mockAuthService },
        { provide: Router, useValue: mockRouter },
        { provide: ActivatedRoute, useValue: { queryParams: of({}) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ForgotPasswordComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should validate matching passwords', () => {
    component.forgotPasswordForm.patchValue({
      newPassword: 'Password123!',
      confirmPassword: 'MismatchPassword'
    });
    expect(component.forgotPasswordForm.hasError('passwordMismatch')).toBeTrue();

    component.forgotPasswordForm.patchValue({
      confirmPassword: 'Password123!'
    });
    expect(component.forgotPasswordForm.hasError('passwordMismatch')).toBeFalse();
  });

  it('should submit successfully', fakeAsync(() => {
    mockAuthService.resetCustomerPassword.and.returnValue(of({ message: 'Success' } as any).pipe(delay(10)));
    component.forgotPasswordForm.patchValue({
      username: 'testuser',
      email: 'test@example.com',
      phoneNumber: '9876543210',
      newPassword: 'Password123!',
      confirmPassword: 'Password123!'
    });

    component.onSubmit();
    expect(component.isSubmitting()).toBeTrue();
    tick(20); 
    expect(component.isSubmitting()).toBeFalse();
    tick(2000); // 1800 timeout
    expect(mockAuthService.resetCustomerPassword).toHaveBeenCalled();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/auth/login']);
  }));

  it('should handle reset password error', () => {
    mockAuthService.resetCustomerPassword.and.returnValue(throwError(() => ({ error: { message: 'Failed' } })));
    component.forgotPasswordForm.patchValue({
        username: 'testuser',
        email: 'test@example.com',
        phoneNumber: '9876543210',
        newPassword: 'Password123!',
        confirmPassword: 'Password123!'
      });

    component.onSubmit();
    expect(component.errorMessage()).toBe('Failed');
    expect(component.isSubmitting()).toBeFalse();
  });

  it('should toggle visibility for passwords', () => {
    expect(component.showNewPassword()).toBeFalse();
    component.toggleNewPasswordVisibility();
    expect(component.showNewPassword()).toBeTrue();

    expect(component.showConfirmPassword()).toBeFalse();
    component.toggleConfirmPasswordVisibility();
    expect(component.showConfirmPassword()).toBeTrue();
  });
});
