import { AfterViewInit, Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { AuthService } from '../../../../core/services';
import { LoginRequest } from '../../../../core/models';
import { CustomValidators } from '../../../../shared/validators/custom-validators';
import { ValidationMessages } from '../../../../shared/helpers/validation-messages';
import { environment } from '../../../../../environments/environment';

interface GoogleCredentialResponse {
  credential?: string;
}

interface GoogleAccountsId {
  initialize: (config: {
    client_id: string;
    callback: (response: GoogleCredentialResponse) => void;
    auto_select?: boolean;
    cancel_on_tap_outside?: boolean;
  }) => void;
  renderButton: (parent: HTMLElement, options: Record<string, unknown>) => void;
}

interface GoogleIdentityServices {
  accounts: {
    id: GoogleAccountsId;
  };
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.html'
  // Styles provided via Tailwind utility classes
})
export class LoginComponent implements OnInit, AfterViewInit, OnDestroy {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  loginForm!: FormGroup;
  isSubmitting = signal(false);
  isGoogleSubmitting = signal(false);
  errorMessage = signal('');
  showPassword = signal(false);
  isGoogleScriptReady = signal(false);
  readonly googleLoginEnabled = environment.enableGoogleLogin && !!environment.googleClientId;
  private googleInitAttempts = 0;
  private readonly maxGoogleInitAttempts = 15;
  private googleInitTimerId: number | null = null;

  ngOnInit(): void {
    this.initializeForm();
    this.checkSessionExpired();
  }

  ngAfterViewInit(): void {
    this.initializeGoogleSignIn();
  }

  ngOnDestroy(): void {
    this.clearGoogleRetryTimer();
  }

  /**
   * Initialize Google button after GIS script becomes available.
   */
  private initializeGoogleSignIn(): void {
    if (!this.googleLoginEnabled) {
      return;
    }

    if (!this.ensureGoogleScriptLoaded()) {
      this.retryGoogleInitialization();
      return;
    }

    const googleIdentity = this.getGoogleIdentityServices();
    const buttonContainer = document.getElementById('google-signin-button');

    if (!googleIdentity || !buttonContainer) {
      this.retryGoogleInitialization();
      return;
    }

    this.clearGoogleRetryTimer();
    this.googleInitAttempts = 0;

    googleIdentity.accounts.id.initialize({
      client_id: environment.googleClientId,
      callback: (response: GoogleCredentialResponse) => this.handleGoogleCredential(response),
      auto_select: false,
      cancel_on_tap_outside: true
    });

    buttonContainer.innerHTML = '';
    googleIdentity.accounts.id.renderButton(buttonContainer, {
      type: 'standard',
      shape: 'pill',
      theme: 'outline',
      text: 'signin_with',
      size: 'large',
      width: 360
    });
    this.isGoogleScriptReady.set(true);
  }

  private ensureGoogleScriptLoaded(): boolean {
    if (this.getGoogleIdentityServices()) {
      return true;
    }

    const existingScript = document.getElementById('google-identity-services-script') as HTMLScriptElement | null;
    if (existingScript) {
      return false;
    }

    const script = document.createElement('script');
    script.id = 'google-identity-services-script';
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.defer = true;
    script.onerror = () => {
      this.errorMessage.set('Unable to load Google sign-in. Please use your username and password.');
    };
    document.head.appendChild(script);
    return false;
  }

  private retryGoogleInitialization(): void {
    if (this.googleInitAttempts >= this.maxGoogleInitAttempts) {
      this.errorMessage.set('Google sign-in is currently unavailable. Please use your username and password.');
      return;
    }

    this.googleInitAttempts += 1;
    this.clearGoogleRetryTimer();
    this.googleInitTimerId = window.setTimeout(() => {
      this.initializeGoogleSignIn();
    }, 300);
  }

  private clearGoogleRetryTimer(): void {
    if (this.googleInitTimerId !== null) {
      window.clearTimeout(this.googleInitTimerId);
      this.googleInitTimerId = null;
    }
  }

  private getGoogleIdentityServices(): GoogleIdentityServices | null {
    const windowWithGoogle = window as Window & { google?: GoogleIdentityServices };
    return windowWithGoogle.google ?? null;
  }

  /**
   * Exchange Google credential for app JWT.
   */
  private handleGoogleCredential(response: GoogleCredentialResponse): void {
    const credential = response.credential;
    if (!credential) {
      this.errorMessage.set('Google sign-in failed. Please try again.');
      return;
    }

    this.errorMessage.set('');
    this.isGoogleSubmitting.set(true);

    this.authService.loginWithGoogle(credential).subscribe({
      next: () => {
        this.isGoogleSubmitting.set(false);
        this.authService.redirectToDashboard();
      },
      error: (error) => {
        this.isGoogleSubmitting.set(false);
        if (error?.status === 404) {
          this.errorMessage.set('Google sign-in is not available yet. Please use your username and password.');
          return;
        }

        this.errorMessage.set(error?.error?.message || error?.message || 'Google sign-in failed. Please try again.');
      }
    });
  }

  private initializeForm(): void {
    this.loginForm = this.fb.group({
      username: ['', [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(50),
        CustomValidators.noWhitespace()
      ]],
      password: ['', [
        Validators.required,
        Validators.minLength(6),
        Validators.maxLength(100)
      ]]
    });
  }

  /**
   * Check if user was redirected due to session expiration
   */
  private checkSessionExpired(): void {
    this.route.queryParams.subscribe(params => {
      if (params['sessionExpired'] === 'true') {
        this.errorMessage.set('Your session has expired. Please login again.');
      }
    });
  }

  /**
   * Toggle password visibility
   */
  togglePasswordVisibility(): void {
    this.showPassword.update(val => !val);
  }

  /**
   * Check if a form control has an error
   */
  hasError(controlName: string, errorName: string): boolean {
    const control = this.loginForm.get(controlName);
    return !!(control && control.hasError(errorName) && (control.dirty || control.touched));
  }

  /**
   * Get validation error message for a field
   */
  getErrorMessage(controlName: string): string {
    const control = this.loginForm.get(controlName);
    if (control && control.errors && (control.touched || control.dirty)) {
      return ValidationMessages.getErrorMessage(controlName, control.errors);
    }
    return '';
  }

  /**
   * Check if field is invalid and should show error
   */
  isFieldInvalid(fieldName: string): boolean {
    const field = this.loginForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  /**
   * Check if field is valid and has been modified
   */
  isFieldValid(fieldName: string): boolean {
    const field = this.loginForm.get(fieldName);
    return !!(field && field.valid && field.dirty);
  }

  /**
   * Handle form submission
   */
  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.markFormGroupTouched(this.loginForm);
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    const credentials: LoginRequest = this.loginForm.value;
    
    // Debug: Log what we're sending to the backend
    console.log('Sending login request with:', credentials);

    this.authService.login(credentials).subscribe({
      next: (response) => {
        this.isSubmitting.set(false);
        this.authService.redirectToDashboard();
      },
      error: (error) => {
        this.isSubmitting.set(false);
        console.error('Login error:', error);
        console.error('Error status:', error.status);
        console.error('Error message:', error.error);
        this.errorMessage.set(error.error?.message || 'Invalid username or password. Please try again.');
      }
    });
  }

  /**
   * Mark all form controls as touched to trigger validation messages
   */
  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();

      if (control instanceof FormGroup) {
        this.markFormGroupTouched(control);
      }
    });
  }
}
