import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CustomerService } from '../../services/customer.service';
import { NotificationPreferenceService } from '../../../../core/services/notification-preference.service';
import { Customer } from '../../../../core/models/customer.model';
import { NotificationPreference, EVENT_LABELS, DEFAULT_EVENT_KEYS } from '../../../../core/models/notification-preference.model';
import { CustomValidators } from '../../../../shared/validators/custom-validators';
import { ValidationMessages } from '../../../../shared/helpers/validation-messages';

@Component({
  selector: 'app-customer-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile.html',
  styleUrls: ['./profile.css']
})
export class ProfileComponent implements OnInit {
  private readonly customerService = inject(CustomerService);
  private readonly preferenceService = inject(NotificationPreferenceService);
  private readonly fb = inject(FormBuilder);

  customer = signal<Customer | null>(null);
  isEditing = signal(false);
  isLoading = signal(false);
  errorMessage = signal('');
  successMessage = signal('');

  // Email notification preferences
  notificationPreferences = signal<NotificationPreference | null>(null);
  isEditingNotifications = signal(false);
  notificationsLoading = signal(false);
  notificationError = signal('');
  notificationSuccess = signal('');
  eventLabels = EVENT_LABELS;
  allEventKeys = DEFAULT_EVENT_KEYS;

  profileForm: FormGroup = this.fb.group({
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
    phoneNumber: ['', [
      Validators.required,
      CustomValidators.phone()
    ]],
    address: ['', [
      Validators.required,
      Validators.minLength(10),
      Validators.maxLength(200)
    ]],
    city: ['', [
      Validators.required,
      Validators.minLength(2),
      Validators.maxLength(50),
      CustomValidators.name()
    ]],
    state: ['', [
      Validators.required,
      Validators.minLength(2),
      Validators.maxLength(50),
      CustomValidators.name()
    ]]
  });

  notificationsForm: FormGroup = this.fb.group({});

  ngOnInit(): void {
    this.loadProfile();
    this.loadNotificationPreferences();
  }

  loadProfile(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.customerService.getMyProfile().subscribe({
      next: (customer) => {
        this.customer.set(customer);
        this.profileForm.patchValue({
          firstName: customer.firstName || customer.first_name || '',
          lastName: customer.lastName || customer.last_name || '',
          phoneNumber: customer.phoneNumber || customer.phone || '',
          address: customer.address,
          city: customer.city,
          state: customer.state
        });
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set('Failed to load profile');
        console.error('Error loading profile:', err);
        this.isLoading.set(false);
      }
    });
  }

  toggleEdit(): void {
    if (this.isEditing()) {
      // Cancel editing - reset form
      const currentCustomer = this.customer();
      this.profileForm.patchValue({
        firstName: currentCustomer?.firstName || currentCustomer?.first_name,
        lastName: currentCustomer?.lastName || currentCustomer?.last_name,
        phoneNumber: currentCustomer?.phoneNumber || currentCustomer?.phone,
        address: currentCustomer?.address,
        city: currentCustomer?.city,
        state: currentCustomer?.state
      });
    }
    this.isEditing.update(val => !val);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  saveProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const updateData = {
      ...this.profileForm.value,
      phone: this.profileForm.get('phoneNumber')?.value // Ensure 'phone' is also sent if backend expects it
    };

    this.customerService.updateMyProfile(updateData).subscribe({
      next: (updatedCustomer) => {
        this.customer.set(updatedCustomer);
        this.isEditing.set(false);
        this.successMessage.set('Profile updated successfully!');
        this.isLoading.set(false);

        // Clear success message after 3 seconds
        setTimeout(() => {
          this.successMessage.set('');
        }, 3000);
      },
      error: (err) => {
        this.errorMessage.set('Failed to update profile');
        console.error('Error updating profile:', err);
        this.isLoading.set(false);
      }
    });
  }

  getFieldError(fieldName: string): string {
    const field = this.profileForm.get(fieldName);
    if (field && field.errors && (field.touched || field.dirty)) {
      return ValidationMessages.getErrorMessage(fieldName, field.errors);
    }
    return '';
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.profileForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  isFieldValid(fieldName: string): boolean {
    const field = this.profileForm.get(fieldName);
    return !!(field && field.valid && field.dirty);
  }

  // Notification preference methods
  private loadNotificationPreferences(): void {
    this.notificationsLoading.set(true);
    this.notificationError.set('');
    
    this.preferenceService.getPreferences().subscribe({
      next: (preferences: NotificationPreference) => {
        this.notificationPreferences.set(preferences);
        this.initializeNotificationsForm(preferences);
        this.notificationsLoading.set(false);
      },
      error: (error: any) => {
        this.notificationError.set('Failed to load notification preferences');
        console.error('Error loading preferences:', error);
        this.notificationsLoading.set(false);
      }
    });
  }

  private initializeNotificationsForm(preferences: NotificationPreference): void {
    const formConfig: any = {
      emailEnabled: [preferences.emailEnabled || false]
    };
    
    // Add event checkboxes
    DEFAULT_EVENT_KEYS.forEach((eventKey: string) => {
      formConfig[eventKey] = [preferences.enabledEventKeys?.includes(eventKey) || false];
    });

    this.notificationsForm = this.fb.group(formConfig);
  }

  toggleNotificationEventPreference(eventKey: string): void {
    const control = this.notificationsForm.get(eventKey);
    if (control) {
      control.setValue(!control.value);
    }
  }

  toggleEditNotifications(): void {
    this.isEditingNotifications.update(val => !val);
    this.notificationError.set('');
    this.notificationSuccess.set('');
  }

  saveNotificationPreferences(): void {
    if (this.notificationsForm.invalid) {
      this.notificationsForm.markAllAsTouched();
      return;
    }

    this.notificationsLoading.set(true);
    this.notificationError.set('');
    this.notificationSuccess.set('');

    const formValues = this.notificationsForm.value;
    const enabledKeys: string[] = [];
    
    DEFAULT_EVENT_KEYS.forEach((eventKey: string) => {
      if (formValues[eventKey]) {
        enabledKeys.push(eventKey);
      }
    });

    const preferences: NotificationPreference = {
      emailEnabled: formValues.emailEnabled,
      enabledEventKeys: enabledKeys
    };

    this.preferenceService.updatePreferences(preferences).subscribe({
      next: (updated: NotificationPreference) => {
        this.notificationPreferences.set(updated);
        this.isEditingNotifications.set(false);
        this.notificationSuccess.set('Notification preferences updated successfully!');
        this.notificationsLoading.set(false);

        // Clear success message after 3 seconds
        setTimeout(() => {
          this.notificationSuccess.set('');
        }, 3000);
      },
      error: (error: any) => {
        this.notificationError.set('Failed to update notification preferences');
        console.error('Error updating preferences:', error);
        this.notificationsLoading.set(false);
      }
    });
  }

  resetNotificationPreferences(): void {
    const prefs = this.notificationPreferences();
    if (prefs) {
      this.initializeNotificationsForm(prefs);
    }
  }
}
