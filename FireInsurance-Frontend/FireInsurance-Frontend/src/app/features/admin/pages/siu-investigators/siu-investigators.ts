import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, SiuInvestigator, SiuInvestigatorRegistrationRequest } from '../../services/admin.service';

@Component({
  selector: 'app-siu-investigators',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './siu-investigators.html'
})
export class SiuInvestigatorsComponent implements OnInit {
  private adminService = inject(AdminService);

  siuInvestigators = signal<SiuInvestigator[]>([]);
  isLoading = signal(true);
  errorMessage = signal('');
  successMessage = signal('');

  showRegisterForm = signal(false);
  isSubmitting = signal(false);

  form: SiuInvestigatorRegistrationRequest = {
    username: '',
    email: '',
    password: '',
    phoneNumber: '',
    firstName: '',
    lastName: '',
    badgeNumber: '',
    specialization: 'GENERAL',
    experienceYears: 0
  };

  specializations = [
    { value: 'GENERAL', label: 'General Investigation' },
    { value: 'FIRE', label: 'Fire-related fraud investigations' },
    { value: 'THEFT', label: 'Theft and burglary investigations' },
    { value: 'FRAUD', label: 'General fraud and suspicious activity' },
    { value: 'PROPERTY', label: 'Property damage investigations' }
  ];

  ngOnInit(): void {
    this.loadSiuInvestigators();
  }

  loadSiuInvestigators(): void {
    this.isLoading.set(true);
    this.adminService.getAllSiuInvestigators().subscribe({
      next: (data) => {
        this.siuInvestigators.set(data || []);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load SIU investigators', err);
        this.errorMessage.set('Failed to load SIU investigators.');
        this.isLoading.set(false);
      }
    });
  }

  toggleForm(): void {
    this.showRegisterForm.set(!this.showRegisterForm());
    this.resetForm();
  }

  registerSiuInvestigator(): void {
    if (!this.form.email || !this.form.password || !this.form.firstName || !this.form.lastName || !this.form.badgeNumber) {
      this.errorMessage.set('Email, password, first name, last name, and badge number are required.');
      setTimeout(() => this.errorMessage.set(''), 4000);
      return;
    }

    // Set username as combination of first and last name for backend compatibility
    this.form.username = this.form.firstName + '_' + this.form.lastName;

    console.log('Attempting to register SIU investigator with data:', {
      ...this.form,
      password: '[REDACTED]'
    });

    this.isSubmitting.set(true);
    this.adminService.registerSiuInvestigator(this.form).subscribe({
      next: (response) => {
        console.log('Registration successful:', response);
        this.successMessage.set('SIU Investigator registered successfully!');
        this.showRegisterForm.set(false);
        this.resetForm();
        this.loadSiuInvestigators();
        setTimeout(() => this.successMessage.set(''), 3500);
        this.isSubmitting.set(false);
      },
      error: (err) => {
        console.error('Registration error:', err);
        console.error('Error status:', err.status);
        console.error('Error body:', err.error);

        let msg = 'Failed to register SIU investigator.';
        if (err.error?.message) {
          msg = err.error.message;
        } else if (err.message) {
          msg = err.message;
        } else if (err.status === 0) {
          msg = 'Cannot connect to server. Please check if the backend is running on port 8081.';
        } else if (err.status === 401) {
          msg = 'Authentication required. Please log in as admin.';
        } else if (err.status === 403) {
          msg = 'Permission denied. Admin access required.';
        }

        this.errorMessage.set(msg);
        setTimeout(() => this.errorMessage.set(''), 6000);
        this.isSubmitting.set(false);
      }
    });
  }

  deactivateInvestigator(investigator: SiuInvestigator): void {
    if (confirm(`Are you sure you want to deactivate ${investigator.firstName} ${investigator.lastName}?`)) {
      this.adminService.deactivateSiuInvestigator(investigator.investigatorId).subscribe({
        next: () => {
          this.successMessage.set(`${investigator.firstName} ${investigator.lastName} has been deactivated.`);
          this.loadSiuInvestigators();
          setTimeout(() => this.successMessage.set(''), 3500);
        },
        error: (err) => {
          const msg = err.error?.message || 'Failed to deactivate investigator.';
          this.errorMessage.set(msg);
          setTimeout(() => this.errorMessage.set(''), 4000);
        }
      });
    }
  }

  activateInvestigator(investigator: SiuInvestigator): void {
    if (confirm(`Are you sure you want to activate ${investigator.firstName} ${investigator.lastName}?`)) {
      this.adminService.activateSiuInvestigator(investigator.investigatorId).subscribe({
        next: () => {
          this.successMessage.set(`${investigator.firstName} ${investigator.lastName} has been activated.`);
          this.loadSiuInvestigators();
          setTimeout(() => this.successMessage.set(''), 3500);
        },
        error: (err) => {
          const msg = err.error?.message || 'Failed to activate investigator.';
          this.errorMessage.set(msg);
          setTimeout(() => this.errorMessage.set(''), 4000);
        }
      });
    }
  }

  private resetForm(): void {
    this.form = {
      username: '',
      email: '',
      password: '',
      phoneNumber: '',
      firstName: '',
      lastName: '',
      badgeNumber: '',
      specialization: 'GENERAL',
      experienceYears: 0
    };
  }

  getSpecializationLabel(specialization: string): string {
    const spec = this.specializations.find(s => s.value === specialization);
    return spec?.label || specialization;
  }

  formatDate(dateString: string | undefined): string {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString();
  }
}