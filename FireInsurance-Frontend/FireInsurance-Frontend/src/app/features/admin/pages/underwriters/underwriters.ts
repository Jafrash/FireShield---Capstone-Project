import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, Underwriter, UnderwriterRegistrationRequest } from '../../services/admin.service';

@Component({
  selector: 'app-underwriters',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './underwriters.html'
})
export class UnderwritersComponent implements OnInit {
  private adminService = inject(AdminService);

  underwriters = signal<Underwriter[]>([]);
  isLoading = signal(true);
  errorMessage = signal('');
  successMessage = signal('');

  showRegisterForm = signal(false);
  isSubmitting = signal(false);

  form: UnderwriterRegistrationRequest = {
    username: '',
    email: '',
    password: '',
    phone: '',
    department: '',
    region: '',
    experienceYears: 0
  };

  ngOnInit(): void {
    this.loadUnderwriters();
  }

  loadUnderwriters(): void {
    this.isLoading.set(true);
    this.adminService.getAllUnderwriters().subscribe({
      next: (data) => {
        this.underwriters.set(data || []);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load underwriters', err);
        this.errorMessage.set('Failed to load underwriters.');
        this.isLoading.set(false);
      }
    });
  }

  toggleForm(): void {
    this.showRegisterForm.set(!this.showRegisterForm());
    this.resetForm();
  }

  registerUnderwriter(): void {
    if (!this.form.username || !this.form.email || !this.form.password) {
      this.errorMessage.set('Username, email and password are required.');
      setTimeout(() => this.errorMessage.set(''), 4000);
      return;
    }
    this.isSubmitting.set(true);
    this.adminService.registerUnderwriter(this.form).subscribe({
      next: () => {
        this.successMessage.set('Underwriter registered successfully!');
        this.showRegisterForm.set(false);
        this.resetForm();
        this.loadUnderwriters();
        setTimeout(() => this.successMessage.set(''), 3500);
        this.isSubmitting.set(false);
      },
      error: (err) => {
        const msg = err.error?.message || 'Failed to register underwriter.';
        this.errorMessage.set(msg);
        setTimeout(() => this.errorMessage.set(''), 4000);
        this.isSubmitting.set(false);
      }
    });
  }

  private resetForm(): void {
    this.form = {
      username: '',
      email: '',
      password: '',
      phone: '',
      department: '',
      region: '',
      experienceYears: 0
    };
  }
}
