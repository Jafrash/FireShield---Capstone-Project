import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { RegisterCustomerRequest, RegisterSurveyorRequest } from '../../../../core/models/user.model';
import { HttpErrorResponse } from '@angular/common/http';
import { CustomValidators } from '../../../../shared/validators/custom-validators';
import { ValidationMessages } from '../../../../shared/helpers/validation-messages';
@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  registerForm: FormGroup;
  isSubmitting = signal(false);
  errorMessage = signal('');
  successMessage = signal('');
  showPassword = signal(false);
  roleSelection = signal<'CUSTOMER' | 'SURVEYOR'>('CUSTOMER');

  constructor() {
    this.registerForm = this.fb.group({
      username: ['', [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(50),
        CustomValidators.username()
      ]],
      firstName: ['', [
        Validators.required,
        Validators.minLength(2),
        Validators.maxLength(50),
        CustomValidators.name()
      ]],
      lastName: ['', [
        Validators.required,
        Validators.minLength(2),
        Validators.maxLength(50),
        CustomValidators.name()
      ]],
      email: ['', [
        Validators.required,
        Validators.email,
        Validators.maxLength(100)
      ]],
      password: ['', [
        Validators.required,
        Validators.minLength(6),
        Validators.maxLength(100)
      ]],
      phoneNumber: ['', [
        Validators.required,
        CustomValidators.phone()
      ]],
      
      // Customer specific
      address: [''],
      city: [''],
      state: [''],

      // Surveyor specific
      licenseNumber: [''],
      experienceYears: [1],
      assignedRegion: ['']
    });

    this.updateRoleValidators('CUSTOMER');
  }

  setRole(role: 'CUSTOMER' | 'SURVEYOR') {
    this.roleSelection.set(role);
    this.updateRoleValidators(role);
  }

  private updateRoleValidators(role: 'CUSTOMER' | 'SURVEYOR') {
    const addressControl = this.registerForm.get('address');
    const cityControl = this.registerForm.get('city');
    const stateControl = this.registerForm.get('state');

    const licenseControl = this.registerForm.get('licenseNumber');
    const expControl = this.registerForm.get('experienceYears');
    const regionControl = this.registerForm.get('assignedRegion');

    if (role === 'CUSTOMER') {
      addressControl?.setValidators([
        Validators.required,
        Validators.minLength(10),
        Validators.maxLength(200)
      ]);
      cityControl?.setValidators([
        Validators.required,
        Validators.minLength(2),
        Validators.maxLength(50),
        CustomValidators.name()
      ]);
      stateControl?.setValidators([
        Validators.required,
        Validators.minLength(2),
        Validators.maxLength(50),
        CustomValidators.name()
      ]);
      
      licenseControl?.clearValidators();
      expControl?.clearValidators();
      regionControl?.clearValidators();
    } else {
      addressControl?.clearValidators();
      cityControl?.clearValidators();
      stateControl?.clearValidators();

      licenseControl?.setValidators([
        Validators.required,
        Validators.maxLength(50)
      ]);
      expControl?.setValidators([
        Validators.required,
        Validators.min(0),
        Validators.max(50)
      ]);
      regionControl?.setValidators([
        Validators.required,
        CustomValidators.name()
      ]);
    }

    addressControl?.updateValueAndValidity();
    cityControl?.updateValueAndValidity();
    stateControl?.updateValueAndValidity();
    
    licenseControl?.updateValueAndValidity();
    expControl?.updateValueAndValidity();
    regionControl?.updateValueAndValidity();
  }

  togglePasswordVisibility(): void {
    this.showPassword.update(val => !val);
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.registerForm.get(controlName);
    return !!(control && control.hasError(errorName) && (control.dirty || control.touched));
  }

  getErrorMessage(controlName: string): string {
    const control = this.registerForm.get(controlName);
    if (control && control.errors && (control.touched || control.dirty)) {
      return ValidationMessages.getErrorMessage(controlName, control.errors);
    }
    return '';
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.registerForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  isFieldValid(fieldName: string): boolean {
    const field = this.registerForm.get(fieldName);
    return !!(field && field.valid && field.dirty);
  }

  isFieldRequired(fieldName: string): boolean {
    const field = this.registerForm.get(fieldName);
    return field?.hasValidator(Validators.required) || false;
  }

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const formValue = this.registerForm.value;

    if (this.roleSelection() === 'CUSTOMER') {
      const request: RegisterCustomerRequest = {
        username: formValue.username,
        firstName: formValue.firstName,
        lastName: formValue.lastName,
        email: formValue.email,
        password: formValue.password,
        phoneNumber: formValue.phoneNumber,
        address: formValue.address,
        city: formValue.city,
        state: formValue.state
      };
      
      this.authService.registerCustomer(request).subscribe({
        next: () => this.handleSuccess(),
        error: (err: HttpErrorResponse) => this.handleError(err)
      });
    } else {
      const request: RegisterSurveyorRequest = {
        username: formValue.username,
        email: formValue.email,
        password: formValue.password,
        phoneNumber: formValue.phoneNumber,
        licenseNumber: formValue.licenseNumber,
        experienceYears: formValue.experienceYears,
        assignedRegion: formValue.assignedRegion
      };

      this.authService.registerSurveyor(request).subscribe({
        next: () => this.handleSuccess(),
        error: (err: HttpErrorResponse) => this.handleError(err)
      });
    }
  }

  private handleSuccess(): void {
    this.isSubmitting.set(false);
    this.successMessage.set('Registration successful! Redirecting to login...');
    setTimeout(() => {
      this.router.navigate(['/auth/login']);
    }, 1500);
  }

  private handleError(error: HttpErrorResponse): void {
    this.isSubmitting.set(false);
    console.error('Registration error:', error);
    this.errorMessage.set(error.error?.message || 'Failed to register. Username or email might already be taken.');
  }
}
