import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CustomerService } from '../../services/customer.service';
import { Property } from '../../../../core/models/property.model';
import { CustomValidators } from '../../../../shared/validators/custom-validators';
import { ValidationMessages } from '../../../../shared/helpers/validation-messages';
import { DocumentUploadComponent } from '../../../../shared/components/ui/document-upload/document-upload.component';

@Component({
  selector: 'app-properties',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DocumentUploadComponent],
  templateUrl: './properties.html',
  styleUrls: ['./properties.css']
})
export class PropertiesComponent implements OnInit {
  private readonly customerService = inject(CustomerService);
  private readonly fb = inject(FormBuilder);

  properties = signal<Property[]>([]);
  isLoading = signal(false);
  errorMessage = signal('');
  successMessage = signal('');
  showAddModal = signal(false);
  showEditModal = signal(false);
  editingProperty = signal<Property | null>(null);

  addPropertyForm: FormGroup = this.fb.group({
    propertyType: ['', [Validators.required]],
    address: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(200), CustomValidators.noWhitespace()]],
    areaSqft: [0, [Validators.required, Validators.min(100), Validators.max(1000000), CustomValidators.positiveNumber()]],
    constructionType: ['', [Validators.required]]
  });

  editPropertyForm: FormGroup = this.fb.group({
    propertyType: ['', [Validators.required]],
    address: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(200), CustomValidators.noWhitespace()]],
    areaSqft: [0, [Validators.required, Validators.min(100), Validators.max(1000000), CustomValidators.positiveNumber()]],
    constructionType: ['', [Validators.required]]
  });

  propertyTypes = [
    'RESIDENTIAL',
    'COMMERCIAL',
    'INDUSTRIAL',
    'APARTMENT'
  ];

  constructionTypes = [
    'BRICK',
    'WOOD',
    'CONCRETE',
    'STEEL',
    'MIXED'
  ];

  ngOnInit(): void {
    this.loadProperties();
  }

  loadProperties(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    
    this.customerService.getMyProperties().subscribe({
      next: (properties) => {
        this.properties.set(properties);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set('Failed to load properties');
        console.error('Error loading properties:', err);
        this.isLoading.set(false);
      }
    });
  }

  openAddModal(): void {
    this.showAddModal.set(true);
    this.addPropertyForm.reset({
      propertyType: '',
      address: '',
      areaSqft: 0,
      constructionType: ''
    });
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  closeAddModal(): void {
    this.showAddModal.set(false);
    this.addPropertyForm.reset();
  }

  openEditModal(property: Property): void {
    this.editingProperty.set(property);
    this.showEditModal.set(true);
    this.editPropertyForm.patchValue({
      propertyType: property.propertyType,
      address: property.address,
      areaSqft: property.areaSqft,
      constructionType: property.constructionType
    });
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  closeEditModal(): void {
    this.showEditModal.set(false);
    this.editingProperty.set(null);
    this.editPropertyForm.reset();
  }

  updateProperty(): void {
    if (this.editPropertyForm.invalid || !this.editingProperty()) {
      this.editPropertyForm.markAllAsTouched();
      return;
    }

    const propertyId = this.editingProperty()!.propertyId;
    this.isLoading.set(true);
    this.errorMessage.set('');
    
    this.customerService.updateProperty(propertyId, this.editPropertyForm.value).subscribe({
      next: (updatedProperty) => {
        this.properties.update(props => 
          props.map(p => p.propertyId === propertyId ? updatedProperty : p)
        );
        this.successMessage.set('Property updated successfully!');
        this.isLoading.set(false);
        this.closeEditModal();
        
        setTimeout(() => {
          this.successMessage.set('');
        }, 3000);
      },
      error: (err) => {
        this.errorMessage.set('Failed to update property');
        console.error('Error updating property:', err);
        this.isLoading.set(false);
      }
    });
  }

  addProperty(): void {
    if (this.addPropertyForm.invalid) {
      this.addPropertyForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');
    
    this.customerService.addProperty(this.addPropertyForm.value).subscribe({
      next: (newProperty) => {
        this.properties.update(props => [...props, newProperty]);
        this.successMessage.set('Property added successfully!');
        this.isLoading.set(false);
        this.closeAddModal();
        
        setTimeout(() => {
          this.successMessage.set('');
        }, 3000);
      },
      error: (err) => {
        this.errorMessage.set('Failed to add property');
        console.error('Error adding property:', err);
        this.isLoading.set(false);
      }
    });
  }

  deleteProperty(propertyId: number): void {
    if (!confirm('Are you sure you want to delete this property?')) {
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');
    
    this.customerService.deleteProperty(propertyId).subscribe({
      next: () => {
        this.properties.update(props => props.filter(p => p.propertyId !== propertyId));
        this.successMessage.set('Property deleted successfully!');
        this.isLoading.set(false);
        
        setTimeout(() => {
          this.successMessage.set('');
        }, 3000);
      },
      error: (err) => {
        this.errorMessage.set('Failed to delete property');
        console.error('Error deleting property:', err);
        this.isLoading.set(false);
      }
    });
  }

  getStatusClass(status: string): string {
    const statusClasses: { [key: string]: string } = {
      'PENDING': 'status-pending',
      'SCHEDULED': 'status-scheduled',
      'COMPLETED': 'status-completed',
      'APPROVED': 'status-approved',
      'REJECTED': 'status-rejected'
    };
    return statusClasses[status] || 'status-default';
  }

  getFieldError(fieldName: string): string {
    const field = this.addPropertyForm.get(fieldName);
    if (field && field.invalid && (field.dirty || field.touched)) {
      return ValidationMessages.getErrorMessage(fieldName, field.errors);
    }
    return '';
  }

  getEditFieldError(fieldName: string): string {
    const field = this.editPropertyForm.get(fieldName);
    if (field && field.invalid && (field.dirty || field.touched)) {
      return ValidationMessages.getErrorMessage(fieldName, field.errors);
    }
    return '';
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.addPropertyForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  isFieldValid(fieldName: string): boolean {
    const field = this.addPropertyForm.get(fieldName);
    return !!(field && field.valid && field.dirty);
  }

  isEditFieldInvalid(fieldName: string): boolean {
    const field = this.editPropertyForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  isEditFieldValid(fieldName: string): boolean {
    const field = this.editPropertyForm.get(fieldName);
    return !!(field && field.valid && field.dirty);
  }
}
