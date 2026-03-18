import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { ForgotPasswordRequest } from '../../../../core/models';
import { CustomValidators } from '../../../../shared/validators/custom-validators';
import { ValidationMessages } from '../../../../shared/helpers/validation-messages';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.css'
})
export class ForgotPasswordComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  forgotPasswordForm: FormGroup = this.fb.group({
    username: ['', [
      Validators.required,
      Validators.minLength(3),
      Validators.maxLength(50),
      CustomValidators.username()
    ]],
    email: ['', [
      Validators.required,
      Validators.email,
      Validators.maxLength(100)
    ]],
    phoneNumber: ['', [
      Validators.required,
      CustomValidators.phone()
    ]],
    newPassword: ['', [
      Validators.required,
      Validators.minLength(6),
      Validators.maxLength(100),
      CustomValidators.noWhitespace()
    ]],
    confirmPassword: ['', [
      Validators.required,
      CustomValidators.noWhitespace()
    ]]
  }, {
    validators: this.passwordsMatchValidator('newPassword', 'confirmPassword')
  });

  isSubmitting = signal(false);
  errorMessage = signal('');
  successMessage = signal('');
  showNewPassword = signal(false);
  showConfirmPassword = signal(false);

  toggleNewPasswordVisibility(): void {
    this.showNewPassword.update(value => !value);
  }

  toggleConfirmPasswordVisibility(): void {
    this.showConfirmPassword.update(value => !value);
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.forgotPasswordForm.get(controlName);
    return !!(control && control.hasError(errorName) && (control.dirty || control.touched));
  }

  getErrorMessage(controlName: string): string {
    const control = this.forgotPasswordForm.get(controlName);
    if (control && control.errors && (control.touched || control.dirty)) {
      return ValidationMessages.getErrorMessage(controlName, control.errors);
    }
    return '';
  }

  passwordsDoNotMatch(): boolean {
    return !!(this.forgotPasswordForm.hasError('passwordMismatch') &&
      (this.forgotPasswordForm.get('confirmPassword')?.touched || this.forgotPasswordForm.get('confirmPassword')?.dirty));
  }

  onSubmit(): void {
    if (this.forgotPasswordForm.invalid) {
      this.forgotPasswordForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const request: ForgotPasswordRequest = {
      username: this.forgotPasswordForm.get('username')?.value,
      email: this.forgotPasswordForm.get('email')?.value,
      phoneNumber: this.forgotPasswordForm.get('phoneNumber')?.value,
      newPassword: this.forgotPasswordForm.get('newPassword')?.value
    };

    this.authService.resetCustomerPassword(request).subscribe({
      next: (response) => {
        this.isSubmitting.set(false);
        this.successMessage.set(response.message || 'Password reset successful. Redirecting to login...');
        this.forgotPasswordForm.reset();
        setTimeout(() => {
          this.router.navigate(['/auth/login']);
        }, 1800);
      },
      error: (error: HttpErrorResponse) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(error.error?.message || 'Unable to reset password. Please verify your customer account details.');
      }
    });
  }

  private passwordsMatchValidator(passwordField: string, confirmPasswordField: string): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const password = control.get(passwordField)?.value;
      const confirmPassword = control.get(confirmPasswordField)?.value;

      if (!password || !confirmPassword) {
        return null;
      }

      return password === confirmPassword ? null : { passwordMismatch: true };
    };
  }
}