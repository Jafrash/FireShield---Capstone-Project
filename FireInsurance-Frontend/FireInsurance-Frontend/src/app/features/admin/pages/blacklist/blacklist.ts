import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AdminService } from '../../services/admin.service';

export interface BlacklistEntry {
  blacklistId: number;
  username: string;
  email: string;
  phoneNumber?: string;
  reason: string;
  active: boolean;
  createdAt: string;
  createdBy: string;
}

@Component({
  selector: 'app-blacklist',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './blacklist.html'
})
export class BlacklistComponent implements OnInit {
  private adminService = inject(AdminService);
  private fb = inject(FormBuilder);

  blacklistedUsers = signal<BlacklistEntry[]>([]);
  filteredUsers = signal<BlacklistEntry[]>([]);
  isLoading = signal<boolean>(true);
  errorMessage = signal<string>('');
  successMessage = signal<string>('');
  searchTerm = signal<string>('');
  showAddForm = signal<boolean>(false);
  isSubmitting = signal<boolean>(false);

  blacklistForm: FormGroup;

  constructor() {
    this.blacklistForm = this.fb.group({
      username: ['', [Validators.maxLength(50)]],
      email: ['', [Validators.email]],
      phoneNumber: [''],
      reason: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(500)]]
    }, { validators: this.atLeastOneIdentifierValidator });
  }

  /**
   * Custom validator: at least one of username/email/phone required
   */
  atLeastOneIdentifierValidator(form: FormGroup) {
    const username = form.get('username')?.value;
    const email = form.get('email')?.value;
    const phone = form.get('phoneNumber')?.value;
    if ((username && username.trim()) || (email && email.trim()) || (phone && phone.trim())) {
      return null;
    }
    return { atLeastOneRequired: true };
  }

  ngOnInit(): void {
    this.loadBlacklistedUsers();
  }

  loadBlacklistedUsers(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.adminService.getBlacklistedUsers().subscribe({
      next: (data) => {
        this.blacklistedUsers.set(data);
        this.filteredUsers.set(data);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading blacklisted users:', error);
        this.errorMessage.set('Failed to load blacklisted users');
        this.isLoading.set(false);
      }
    });
  }

  toggleAddForm(): void {
    this.showAddForm.set(!this.showAddForm());
    if (this.showAddForm()) {
      this.blacklistForm.reset();
      this.errorMessage.set('');
      this.successMessage.set('');
    }
  }

  onSubmit(): void {
    if (this.blacklistForm.invalid || this.isSubmitting()) {
      this.errorMessage.set('At least one of username, email, or phone is required.');
      return;
    }
    this.isSubmitting.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const formData = this.blacklistForm.value;

    this.adminService.addToBlacklist(formData).subscribe({
      next: (response) => {
        this.successMessage.set('User successfully added to blacklist');
        this.blacklistForm.reset();
        this.showAddForm.set(false);
        this.loadBlacklistedUsers();
        this.isSubmitting.set(false);
      },
      error: (error) => {
        console.error('Error adding to blacklist:', error);
        this.errorMessage.set(error.error?.message || 'Failed to add user to blacklist');
        this.isSubmitting.set(false);
      }
    });
  }

  onSearch(): void {
    const term = this.searchTerm().toLowerCase();
    if (!term) {
      this.filteredUsers.set(this.blacklistedUsers());
    } else {
      const filtered = this.blacklistedUsers().filter(user =>
        user.username.toLowerCase().includes(term) ||
        user.email.toLowerCase().includes(term) ||
        (user.phoneNumber && user.phoneNumber.toLowerCase().includes(term)) ||
        user.reason.toLowerCase().includes(term) ||
        user.createdBy.toLowerCase().includes(term)
      );
      this.filteredUsers.set(filtered);
    }
  }

  removeFromBlacklist(id: number): void {
    if (confirm('Are you sure you want to remove this user from the blacklist?')) {
      this.adminService.removeFromBlacklist(id).subscribe({
        next: () => {
          this.successMessage.set('User removed from blacklist successfully');
          this.loadBlacklistedUsers();
        },
        error: (error) => {
          console.error('Error removing from blacklist:', error);
          this.errorMessage.set('Failed to remove user from blacklist');
        }
      });
    }
  }

  clearMessages(): void {
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  get formControls() {
    return this.blacklistForm.controls;
  }
}