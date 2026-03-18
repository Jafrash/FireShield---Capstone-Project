import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AdminService, Surveyor } from '../../../../features/admin/services/admin.service';
import { Property } from '../../../../core/models/property.model';
import { Claim } from '../../../../core/models/claim.model';

@Component({
  selector: 'app-surveyors',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './surveyors.html'
})
export class SurveyorsComponent implements OnInit {
  private adminService = inject(AdminService);
  private fb = inject(FormBuilder);
  
  surveyors = signal<Surveyor[]>([]);
  filteredSurveyors = signal<Surveyor[]>([]);
  properties = signal<Property[]>([]);
  claims = signal<Claim[]>([]);

  isLoading = signal<boolean>(true);
  isAssigning = signal<boolean>(false);
  errorMessage = signal<string>('');
  assignmentSuccess = signal<string>('');
  assignmentError = signal<string>('');
  searchTerm = signal<string>('');

  showRegisterForm = signal<boolean>(false);
  isSubmitting = signal<boolean>(false);
  successMessage = signal<string>('');

  registerForm: any = {
    username: '',
    email: '',
    password: '',
    phoneNumber: '',
    licenseNumber: '',
    experienceYears: 0,
    assignedRegion: ''
  };

  ngOnInit(): void {
    this.loadSurveyors();
  }

  loadSurveyors(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.adminService.getAllSurveyors().subscribe({
      next: (data) => {
        this.surveyors.set(data);
        this.filteredSurveyors.set(data);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading surveyors:', error);
        this.errorMessage.set('Failed to load surveyors');
        this.isLoading.set(false);
      }
    });
  }

  toggleForm(): void {
    this.showRegisterForm.set(!this.showRegisterForm());
    if (!this.showRegisterForm()) {
      this.resetForm();
    }
  }

  resetForm(): void {
    this.registerForm = {
      username: '',
      email: '',
      password: '',
      phoneNumber: '',
      licenseNumber: '',
      experienceYears: 0,
      assignedRegion: ''
    };
  }

  registerSurveyor(): void {
    const { username, email, password } = this.registerForm;
    if (!username || !email || !password) {
      this.errorMessage.set('Username, email and password are required.');
      setTimeout(() => this.errorMessage.set(''), 4000);
      return;
    }

    this.isSubmitting.set(true);
    this.adminService.createSurveyor(this.registerForm).subscribe({
      next: () => {
        this.successMessage.set('Surveyor registered successfully!');
        this.showRegisterForm.set(false);
        this.resetForm();
        this.loadSurveyors();
        setTimeout(() => this.successMessage.set(''), 3500);
        this.isSubmitting.set(false);
      },
      error: (error) => {
        console.error('Error registering surveyor:', error);
        const msg = error.error?.message || 'Failed to register surveyor.';
        this.errorMessage.set(msg);
        setTimeout(() => this.errorMessage.set(''), 4000);
        this.isSubmitting.set(false);
      }
    });
  }

  searchSurveyors(term: string): void {
    this.searchTerm.set(term);
    const lowerTerm = term.toLowerCase();
    
    if (!lowerTerm) {
      this.filteredSurveyors.set(this.surveyors());
      return;
    }

    const filtered = this.surveyors().filter(surveyor =>
      (surveyor.firstName || '').toLowerCase().includes(lowerTerm) ||
      (surveyor.lastName || '').toLowerCase().includes(lowerTerm) ||
      (surveyor.email || '').toLowerCase().includes(lowerTerm) ||
      (surveyor.username || '').toLowerCase().includes(lowerTerm) ||
      (surveyor.specialization || '').toLowerCase().includes(lowerTerm)
    );

    this.filteredSurveyors.set(filtered);
  }

  deleteSurveyor(id: number): void {
    if (!confirm('Are you sure you want to delete this surveyor?')) {
      return;
    }

    this.adminService.deleteSurveyor(id).subscribe({
      next: () => {
        const updated = this.surveyors().filter(s => s.surveyorId !== id);
        this.surveyors.set(updated);
        this.filteredSurveyors.set(updated);
      },
      error: (error) => {
        console.error('Error deleting surveyor:', error);
        alert('Failed to delete surveyor');
      }
    });
  }
}

