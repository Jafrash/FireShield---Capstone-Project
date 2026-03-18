export class ValidationMessages {
  static getErrorMessage(controlName: string, errors: any): string {
    if (!errors) return '';

    if (errors['required']) {
      return `${this.formatFieldName(controlName)} is required`;
    }
    
    if (errors['email']) {
      return 'Please enter a valid email address';
    }
    
    if (errors['minlength']) {
      return `Minimum ${errors['minlength'].requiredLength} characters required`;
    }
    
    if (errors['maxlength']) {
      return `Maximum ${errors['maxlength'].requiredLength} characters allowed`;
    }
    
    if (errors['min']) {
      return `Minimum value is ${errors['min'].min}`;
    }
    
    if (errors['max']) {
      return `Maximum value is ${errors['max'].max}`;
    }
    
    if (errors['minAmount']) {
      return `Minimum amount is ₹${errors['minAmount'].min.toLocaleString()}`;
    }
    
    if (errors['maxAmount']) {
      return `Maximum amount is ₹${errors['maxAmount'].max.toLocaleString()}`;
    }
    
    if (errors['invalidPhone']) {
      return 'Enter a valid 10-digit mobile number starting with 6-9';
    }
    
    if (errors['invalidName']) {
      return 'Only letters and spaces are allowed';
    }
    
    if (errors['invalidFormat']) {
      return 'Invalid format - only letters and numbers allowed';
    }

    if (errors['invalidUsername']) {
      return 'Username can only contain letters, numbers, hyphens and underscores';
    }
    
    if (errors['futureDate']) {
      return 'Date cannot be in the future';
    }
    
    if (errors['whitespace']) {
      return 'Field cannot be empty or contain only spaces';
    }

    if (errors['notPositive']) {
      return 'Value must be greater than 0';
    }
    
    if (errors['pattern']) {
      return 'Invalid format';
    }

    return 'Invalid value';
  }

  private static formatFieldName(name: string): string {
    // Convert camelCase to Title Case with spaces
    return name
      .replace(/([A-Z])/g, ' $1')
      .replace(/^./, str => str.toUpperCase())
      .trim();
  }
}
