import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { AdminService, SiuInvestigator, SiuInvestigatorRegistrationRequest } from '../../services/admin.service';

@Component({
  selector: 'app-siu-investigators',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './siu-investigators.html'
})
export class SiuInvestigatorsComponent implements OnInit {
  private adminService = inject(AdminService);

  investigators = signal<SiuInvestigator[]>([]);
  filteredInvestigators = signal<SiuInvestigator[]>([]);

  isLoading = signal<boolean>(true);
  errorMessage = signal<string>('');
  searchTerm = signal<string>('');

  showRegisterForm = signal<boolean>(false);
  isSubmitting = signal<boolean>(false);
  successMessage = signal<string>('');

  registerForm: SiuInvestigatorRegistrationRequest = {
    username: '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    phone: '',
    badgeNumber: '',
    department: '',
    experienceYears: 0,
    specialization: ''
  };

  ngOnInit(): void {
    this.loadInvestigators();
  }

  loadInvestigators(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.adminService.getAllSiuInvestigators().subscribe({
      next: (data) => {
        this.investigators.set(data);
        this.filteredInvestigators.set(data);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading SIU investigators:', error);
        this.errorMessage.set('Failed to load SIU investigators');
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
      firstName: '',
      lastName: '',
      phone: '',
      badgeNumber: '',
      department: '',
      experienceYears: 0,
      specialization: ''
    };
  }

  registerInvestigator(): void {
    const { username, email, password } = this.registerForm;
    if (!username || !email || !password) {
      this.errorMessage.set('Username, email and password are required.');
      setTimeout(() => this.errorMessage.set(''), 4000);
      return;
    }

    this.isSubmitting.set(true);
    this.adminService.registerSiuInvestigator(this.registerForm).subscribe({
      next: () => {
        this.successMessage.set('SIU Investigator registered successfully!');
        this.showRegisterForm.set(false);
        this.resetForm();
        this.loadInvestigators();
        setTimeout(() => this.successMessage.set(''), 3500);
        this.isSubmitting.set(false);
      },
      error: (error) => {
        console.error('Error registering SIU investigator:', error);
        const msg = error.error?.message || 'Failed to register SIU investigator.';
        this.errorMessage.set(msg);
        setTimeout(() => this.errorMessage.set(''), 4000);
        this.isSubmitting.set(false);
      }
    });
  }

  searchInvestigators(term: string): void {
    this.searchTerm.set(term);
    const lowerTerm = term.toLowerCase();

    if (!lowerTerm) {
      this.filteredInvestigators.set(this.investigators());
      return;
    }

    const filtered = this.investigators().filter(investigator =>
      (investigator.firstName || '').toLowerCase().includes(lowerTerm) ||
      (investigator.lastName || '').toLowerCase().includes(lowerTerm) ||
      (investigator.email || '').toLowerCase().includes(lowerTerm) ||
      (investigator.username || '').toLowerCase().includes(lowerTerm) ||
      (investigator.badgeNumber || '').toLowerCase().includes(lowerTerm) ||
      (investigator.department || '').toLowerCase().includes(lowerTerm) ||
      (investigator.specialization || '').toLowerCase().includes(lowerTerm)
    );

    this.filteredInvestigators.set(filtered);
  }

  deleteInvestigator(id: number): void {
    if (!confirm('Are you sure you want to delete this SIU investigator?')) {
      return;
    }

    this.adminService.deleteSiuInvestigator(id).subscribe({
      next: () => {
        const updated = this.investigators().filter(i => i.investigatorId !== id);
        this.investigators.set(updated);
        this.filteredInvestigators.set(updated);
        this.successMessage.set('SIU Investigator deleted successfully!');
        setTimeout(() => this.successMessage.set(''), 3500);
      },
      error: (error) => {
        console.error('Error deleting SIU investigator:', error);
        this.errorMessage.set('Failed to delete SIU investigator');
        setTimeout(() => this.errorMessage.set(''), 4000);
      }
    });
  }
}