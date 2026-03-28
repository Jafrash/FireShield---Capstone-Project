import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { RegisterComponent } from './register';
import { AuthService } from '../../services/auth.service';
import { provideRouter, ActivatedRoute, Router } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { of, throwError, delay } from 'rxjs';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let mockAuthService: jasmine.SpyObj<AuthService>;
  let mockRouter: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    mockAuthService = jasmine.createSpyObj('AuthService', ['registerCustomer', 'registerSurveyor']);
    mockRouter = jasmine.createSpyObj('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [RegisterComponent, ReactiveFormsModule],
      providers: [
        { provide: AuthService, useValue: mockAuthService },
        { provide: Router, useValue: mockRouter },
        { provide: ActivatedRoute, useValue: { queryParams: of({}) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should switch roles and update validators', () => {
    expect(component.roleSelection()).toBe('CUSTOMER');
    expect(component.registerForm.get('address')?.validator).toBeTruthy();
    expect(component.registerForm.get('licenseNumber')?.validator).toBeNull();

    component.setRole('SURVEYOR');
    expect(component.roleSelection()).toBe('SURVEYOR');
    expect(component.registerForm.get('address')?.validator).toBeNull();
    expect(component.registerForm.get('licenseNumber')?.validator).toBeTruthy();
  });

  it('should validate customer form fields', () => {
    component.setRole('CUSTOMER');
    const form = component.registerForm;
    
    form.patchValue({
        username: 'usr',
        firstName: 'Jo',
        lastName: 'Doe',
        email: 'jo@doe.com',
        password: 'password123',
        phoneNumber: '9876543210',
        address: '123 Test Street, New York',
        city: 'New York',
        state: 'NY'
    });

    expect(form.valid).toBeTrue();
  });

  it('should handle successful registration for customer', fakeAsync(() => {
    mockAuthService.registerCustomer.and.returnValue(of({} as any).pipe(delay(10)));
    component.registerForm.patchValue({
        username: 'customer1',
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@doe.com',
        password: 'password123',
        phoneNumber: '9876543210',
        address: '123 Address Street, City, State',
        city: 'City',
        state: 'State'
    });

    component.onSubmit();
    expect(component.isSubmitting()).toBeTrue();
    tick(50);
    expect(component.isSubmitting()).toBeFalse();
    tick(2000); // 1500 timeout
    expect(mockAuthService.registerCustomer).toHaveBeenCalled();
    expect(mockRouter.navigate).toHaveBeenCalledWith(['/auth/login']);
  }));

  it('should handle registration error', () => {
    mockAuthService.registerCustomer.and.returnValue(throwError(() => ({ error: { message: 'Username taken' } })));
    component.registerForm.patchValue({
        username: 'existing',
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@doe.com',
        password: 'password123',
        phoneNumber: '9876543210',
        address: '123 Address Street, City, State',
        city: 'City',
        state: 'State'
    });

    component.onSubmit();
    expect(component.errorMessage()).toBe('Username taken');
    expect(component.isSubmitting()).toBeFalse();
  });

  it('should toggle password visibility', () => {
    expect(component.showPassword()).toBeFalse();
    component.togglePasswordVisibility();
    expect(component.showPassword()).toBeTrue();
  });
});
