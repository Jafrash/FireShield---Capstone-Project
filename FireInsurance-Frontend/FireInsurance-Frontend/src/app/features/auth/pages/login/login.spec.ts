import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { LoginComponent } from './login';
import { AuthService } from '../../../../core/services';
import { ReactiveFormsModule } from '@angular/forms';
import { of, throwError, delay, timer, switchMap } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let mockAuthService: jasmine.SpyObj<AuthService>;
  let mockRouter: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    mockAuthService = jasmine.createSpyObj('AuthService', ['login', 'loginWithGoogle', 'redirectToDashboard']);
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);
    
    await TestBed.configureTestingModule({
      imports: [LoginComponent, ReactiveFormsModule],
      providers: [
        { provide: AuthService, useValue: mockAuthService },
        { provide: Router, useValue: mockRouter },
        { 
          provide: ActivatedRoute, 
          useValue: { queryParams: of({ sessionExpired: 'true' }) }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and check session expired', () => {
    expect(component).toBeTruthy();
    expect(component.errorMessage()).toBe('Your session has expired. Please login again.');
  });

  it('should initialize form with empty values', () => {
    expect(component.loginForm.get('username')?.value).toBe('');
    expect(component.loginForm.get('password')?.value).toBe('');
    expect(component.loginForm.invalid).toBeTrue();
  });

  it('should validate username and password requirements', () => {
    const usernameCtrl = component.loginForm.get('username');
    const passwordCtrl = component.loginForm.get('password');
    
    // Required
    usernameCtrl?.setValue('');
    passwordCtrl?.setValue('');
    expect(usernameCtrl?.hasError('required')).toBeTrue();
    expect(passwordCtrl?.hasError('required')).toBeTrue();
    
    // Min length
    usernameCtrl?.setValue('ab');
    passwordCtrl?.setValue('12345');
    expect(usernameCtrl?.hasError('minlength')).toBeTrue();
    expect(passwordCtrl?.hasError('minlength')).toBeTrue();
    
    // Valid
    usernameCtrl?.setValue('validuser');
    passwordCtrl?.setValue('validPassword123');
    expect(usernameCtrl?.valid).toBeTrue();
    expect(passwordCtrl?.valid).toBeTrue();
    expect(component.loginForm.valid).toBeTrue();
  });

  it('should toggle password visibility', () => {
    expect(component.showPassword()).toBeFalse();
    component.togglePasswordVisibility();
    expect(component.showPassword()).toBeTrue();
  });

  it('should not submit if form is invalid', () => {
    component.onSubmit();
    expect(mockAuthService.login).not.toHaveBeenCalled();
    expect(component.loginForm.get('username')?.touched).toBeTrue();
  });

  it('should submit successfully and redirect', () => {
    component.loginForm.setValue({ username: 'testuser', password: 'password123' });
    fixture.detectChanges();
    mockAuthService.login.and.returnValue(of({ token: 'abc' } as any));
    
    component.onSubmit();
    
    expect(component.isSubmitting()).toBeFalse();
    expect(mockAuthService.login).toHaveBeenCalledWith({ username: 'testuser', password: 'password123' });
    expect(mockAuthService.redirectToDashboard).toHaveBeenCalled();
  });

  it('should handle login error gracefully', fakeAsync(() => {
    component.loginForm.setValue({ username: 'testuser', password: 'password123' });
    fixture.detectChanges();
    mockAuthService.login.and.returnValue(timer(10).pipe(switchMap(() => throwError(() => ({ error: { message: 'Invalid credentials' } })))));
    
    component.onSubmit();
    // Use truthy to be safe? No, should be true.
    expect(component.isSubmitting()).toBeTrue();
    tick(50); 
    fixture.detectChanges();
    expect(component.errorMessage()).toBe('Invalid credentials');
    expect(component.isSubmitting()).toBeFalse();
  }));

  it('should correctly expose error messages parsing', () => {
    const usernameCtrl = component.loginForm.get('username');
    usernameCtrl?.setValue('');
    usernameCtrl?.markAsTouched();
    
    expect(component.isFieldInvalid('username')).toBeTrue();
    expect(component.hasError('username', 'required')).toBeTrue();
  });
});
