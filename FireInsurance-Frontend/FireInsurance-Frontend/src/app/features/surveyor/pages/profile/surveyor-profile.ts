import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { SurveyorService, UpdateSurveyorRequest } from '../../services/surveyor.service';
import { CustomValidators } from '../../../../shared/validators/custom-validators';
import { ValidationMessages } from '../../../../shared/helpers/validation-messages';

interface SurveyorProfile {
  surveyorId: number;
  username: string;
  email: string;
  phoneNumber?: string;
  licenseNumber?: string;
  experienceYears?: number;
  assignedRegion?: string;
  createdAt?: string;
}

@Component({
  selector: 'app-surveyor-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './surveyor-profile.html',
  styleUrls: ['./surveyor-profile.css']
})
export class SurveyorProfileComponent implements OnInit {
  private readonly surveyorService = inject(SurveyorService);
  private readonly fb = inject(FormBuilder);

  surveyor = signal<SurveyorProfile | null>(null);
  isEditing = signal(false);
  isLoading = signal(false);
  errorMessage = signal('');
  successMessage = signal('');

  profileForm: FormGroup = this.fb.group({
    phoneNumber: ['', [CustomValidators.phone()]],
    licenseNumber: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(50), CustomValidators.alphanumeric()]],
    experienceYears: [0, [Validators.required, Validators.min(0), Validators.max(50), CustomValidators.positiveNumber()]],
    assignedRegion: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100), CustomValidators.noWhitespace()]]
  });

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    
    this.surveyorService.getMyProfile().subscribe({
      next: (surveyor) => {
        this.surveyor.set(surveyor);
        this.profileForm.patchValue({
          phoneNumber: surveyor.phoneNumber || '',
          licenseNumber: surveyor.licenseNumber || '',
          experienceYears: surveyor.experienceYears || 0,
          assignedRegion: surveyor.assignedRegion || ''
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
    this.isEditing.set(!this.isEditing());
    if (!this.isEditing()) {
      // Reset form when canceling
      this.loadProfile();
      this.errorMessage.set('');
      this.successMessage.set('');
    }
  }

  updateProfile(): void {
    if (this.profileForm.invalid) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');
    
    const updateData: UpdateSurveyorRequest = this.profileForm.value;
    
    this.surveyorService.updateMyProfile(updateData).subscribe({
      next: (updatedSurveyor) => {
        this.surveyor.set(updatedSurveyor);
        this.successMessage.set('Profile updated successfully!');
        this.isEditing.set(false);
        this.isLoading.set(false);
        
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
    if (field && field.invalid && (field.dirty || field.touched)) {
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
}
