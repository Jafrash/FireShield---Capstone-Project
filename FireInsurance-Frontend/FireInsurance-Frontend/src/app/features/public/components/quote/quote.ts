import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { PublicService } from '../../services/public.service';
import { CustomValidators } from '../../../../shared/validators/custom-validators';
import { ValidationMessages } from '../../../../shared/helpers/validation-messages';

@Component({
  selector: 'app-quote',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './quote.html'
})
export class QuoteComponent {
  private fb = inject(FormBuilder);
  private publicService = inject(PublicService);

  quoteForm: FormGroup;
  isSubmitting = false;
  successMessage = '';
  errorMessage = '';

  constructor() {
    this.quoteForm = this.fb.group({
      propertyType: ['', [Validators.required]],
      buildingArea: ['', [Validators.required, Validators.min(100), Validators.max(1000000), CustomValidators.positiveNumber()]],
      constructionType: ['', [Validators.required]],
      city: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50), CustomValidators.name()]]
    });
  }

  onSubmit() {
    if (this.quoteForm.invalid) {
      this.quoteForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.successMessage = '';
    this.errorMessage = '';

    this.publicService.requestQuote(this.quoteForm.value).subscribe({
      next: (res) => {
        this.isSubmitting = false;
        this.successMessage = 'Your quote request has been submitted successfully! We will contact you soon.';
        this.quoteForm.reset();
      },
      error: (err) => {
        // Since the backend might not have the /quotes endpoint implemented yet,
        // we show a descriptive fallback or the actual error.
        this.isSubmitting = false;
        this.errorMessage = err.error?.message || 'Failed to submit quote request. The endpoint might not be ready yet.';
      }
    });
  }

  hasError(controlName: string, errorName: string): boolean {
    const control = this.quoteForm.get(controlName);
    return !!(control && control.hasError(errorName) && (control.dirty || control.touched));
  }

  getErrorMessage(controlName: string): string {
    const field = this.quoteForm.get(controlName);
    if (field && field.invalid && (field.dirty || field.touched)) {
      return ValidationMessages.getErrorMessage(controlName, field.errors);
    }
    return '';
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.quoteForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  isFieldValid(fieldName: string): boolean {
    const field = this.quoteForm.get(fieldName);
    return !!(field && field.valid && field.dirty);
  }
}
