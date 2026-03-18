import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export class CustomValidators {
  
  // Phone number validator (Indian format: 10 digits starting with 6-9)
  static phone(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      const phoneRegex = /^[6-9]\d{9}$/;
      return phoneRegex.test(control.value.toString()) ? null : { invalidPhone: true };
    };
  }

  // Name validator (letters and spaces only)
  static name(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      const nameRegex = /^[a-zA-Z\s]+$/;
      return nameRegex.test(control.value) ? null : { invalidName: true };
    };
  }

  // No future date
  static noFutureDate(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      const inputDate = new Date(control.value);
      const today = new Date();
      today.setHours(23, 59, 59, 999);
      return inputDate <= today ? null : { futureDate: true };
    };
  }

  // Min amount
  static minAmount(min: number): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value && control.value !== 0) return null;
      return Number(control.value) >= min ? null : { minAmount: { min, actual: control.value } };
    };
  }

  // Max amount
  static maxAmount(max: number): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value && control.value !== 0) return null;
      return Number(control.value) <= max ? null : { maxAmount: { max, actual: control.value } };
    };
  }

  // No whitespace only
  static noWhitespace(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      const isWhitespace = (control.value || '').trim().length === 0;
      return !isWhitespace ? null : { whitespace: true };
    };
  }

  // Alphanumeric with spaces
  static alphanumeric(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      const regex = /^[a-zA-Z0-9\s]+$/;
      return regex.test(control.value) ? null : { invalidFormat: true };
    };
  }

  // Username validator (alphanumeric, underscore, hyphen)
  static username(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      const regex = /^[a-zA-Z0-9_-]+$/;
      return regex.test(control.value) ? null : { invalidUsername: true };
    };
  }

  // Positive number validator
  static positiveNumber(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value && control.value !== 0) return null;
      return Number(control.value) > 0 ? null : { notPositive: true };
    };
  }
}
