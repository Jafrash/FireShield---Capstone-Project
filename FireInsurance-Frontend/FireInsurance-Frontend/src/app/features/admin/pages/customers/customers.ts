import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../../features/admin/services/admin.service';
import { AdminCustomerView } from '../../../../core/models/customer.model';

@Component({
  selector: 'app-customers',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './customers.html'
})
export class CustomersComponent implements OnInit {
  private adminService = inject(AdminService);
  
  customers = signal<AdminCustomerView[]>([]);
  filteredCustomers = signal<AdminCustomerView[]>([]);
  isLoading = signal<boolean>(true);
  errorMessage = signal<string>('');
  searchTerm = signal<string>('');

  ngOnInit(): void {
    this.loadCustomers();
  }

  loadCustomers(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.adminService.getAllCustomers().subscribe({
      next: (data) => {
        // Convert Customer[] to AdminCustomerView[] with default values
        const adminCustomers: AdminCustomerView[] = data.map(customer => ({
          ...customer,
          propertiesCount: 0,
          policiesCount: 0,
          active: true
        }));
        this.customers.set(adminCustomers);
        this.filteredCustomers.set(adminCustomers);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading customers:', error);
        this.errorMessage.set('Failed to load customers');
        this.isLoading.set(false);
      }
    });
  }

  searchCustomers(term: string): void {
    this.searchTerm.set(term);
    if (!term) {
      this.filteredCustomers.set(this.customers());
      return;
    }

    const filtered = this.customers().filter(customer =>
      customer.firstName?.toLowerCase().includes(term.toLowerCase()) ||
      customer.lastName?.toLowerCase().includes(term.toLowerCase()) ||
      customer.email?.toLowerCase().includes(term.toLowerCase()) ||
      customer.username?.toLowerCase().includes(term.toLowerCase())
    );
    this.filteredCustomers.set(filtered);
  }

  deleteCustomer(id: number): void {
    if (!confirm('Are you sure you want to delete this customer?')) {
      return;
    }

    this.adminService.deleteCustomer(id).subscribe({
      next: () => {
        const updated = this.customers().filter(c => c.id !== id);
        this.customers.set(updated);
        this.searchCustomers(this.searchTerm());
      },
      error: (error) => {
        console.error('Error deleting customer:', error);
        alert('Failed to delete customer');
      }
    });
  }
}
