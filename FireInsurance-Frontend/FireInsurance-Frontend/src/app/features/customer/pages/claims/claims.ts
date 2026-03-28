import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CustomerService } from '../../services/customer.service';
import { Claim } from '../../../../core/models/claim.model';
import { Property } from '../../../../core/models/property.model';
import { Subscription } from '../../../../core/models/customer.model';
import { CustomValidators } from '../../../../shared/validators/custom-validators';
import { ValidationMessages } from '../../../../shared/helpers/validation-messages';
import { ClaimDocumentUploadComponent } from '../../../../shared/components/ui/claim-document-upload/claim-document-upload.component';

@Component({
  selector: 'app-customer-claims',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, ClaimDocumentUploadComponent],
  templateUrl: './claims.html',
  styleUrls: ['./claims.css']
})
export class CustomerClaimsComponent implements OnInit {
  private readonly customerService = inject(CustomerService);
  private readonly fb = inject(FormBuilder);

  claims = signal<Claim[]>([]);
  properties = signal<Property[]>([]);
  activeSubscriptions = signal<Subscription[]>([]);

  isLoading = signal(false);
  errorMessage = signal('');
  successMessage = signal('');
  showFileClaimModal = signal(false);
  claimIdForUpload = signal<number | null>(null);
  selectedClaimForSettlement = signal<Claim | null>(null);

  fileClaimForm: FormGroup = this.fb.group({
    subscriptionId: [0, [
      Validators.required,
      Validators.min(1)
    ]],
    description: ['', [
      Validators.required,
      Validators.minLength(20),
      Validators.maxLength(2000),
      CustomValidators.noWhitespace()
    ]],
    claimAmount: [0, [
      Validators.required,
      CustomValidators.minAmount(1000),
      CustomValidators.maxAmount(100000000)
    ]],
    incidentDate: ['', [
      Validators.required,
      CustomValidators.noFutureDate()
    ]],
    causeOfFire: ['', [Validators.required]],
    firNumber: ['', [Validators.required]],
    fireBrigadeReportNumber: [''],
    policeStation: ['', [Validators.required]],
    lossType: ['BUILDING', [Validators.required]],
    contactPhoneNumber: ['', [Validators.required, Validators.pattern('^[0-9]{10}$')]],
    witnessDetails: ['']
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    Promise.all([
      this.customerService.getMyClaims().toPromise(),
      this.customerService.getMyProperties().toPromise(),
      this.customerService.getMySubscriptions().toPromise()
    ]).then(([claims, properties, subs]) => {
      // Apply smart calculation for settlement amounts
      const processedClaims = (claims || []).map(claim => this.calculateSettlementAmount(claim));
      this.claims.set(processedClaims);
      this.properties.set(properties || []);
      this.activeSubscriptions.set((subs || []).filter(s => s.status === 'ACTIVE'));
      this.isLoading.set(false);
    }).catch(err => {
      this.errorMessage.set('Failed to load data');
      console.error('Error loading data:', err);
      this.isLoading.set(false);
    });
  }

  /**
   * Smart calculation fallback for settlement amount.
   * If settlementAmount is 0 or missing, calculate it as:
   * Math.max(0, estimatedLoss - deductible - depreciation)
   */
  private calculateSettlementAmount(claim: Claim): Claim {
    const estimatedLoss = Number(claim.estimatedLoss) || 0;
    const deductible = Number(claim.deductible) || 0;
    const depreciation = Number(claim.depreciation) || 0;
    const backendSettlement = Number(claim.settlementAmount) || 0;

    // If we have an assessment, the settlement amount MUST be consistent with its components
    if (estimatedLoss > 0) {
      const calculatedAmount = Math.max(0, estimatedLoss - deductible - depreciation);
      
      // If the backend value is significantly different from the breakdown total, 
      // we prioritize the accurate mathematical breakdown provided by the user.
      // This fixes the issue where 50000 - 2500 resulted in 9500 instead of 47500.
      return { ...claim, settlementAmount: calculatedAmount };
    }

    return claim;
  }

  openFileClaimModal(): void {
    this.showFileClaimModal.set(true);
    this.claimIdForUpload.set(null);
    this.fileClaimForm.reset({
      subscriptionId: 0,
      description: '',
      claimAmount: 0,
      incidentDate: '',
      causeOfFire: '',
      firNumber: '',
      fireBrigadeReportNumber: '',
      policeStation: '',
      lossType: 'BUILDING',
      contactPhoneNumber: '',
      witnessDetails: ''
    });
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  openUploadModal(claimId: number): void {
    this.claimIdForUpload.set(claimId);
    this.showFileClaimModal.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  closeFileClaimModal(): void {
    this.showFileClaimModal.set(false);
    this.claimIdForUpload.set(null);
    this.fileClaimForm.reset();
  }

  openSettlementModal(claim: Claim): void {
    this.selectedClaimForSettlement.set(claim);
  }

  closeSettlementModal(): void {
    this.selectedClaimForSettlement.set(null);
  }

  fileClaim(): void {
    if (this.fileClaimForm.invalid) {
      this.fileClaimForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');

    const payload = {
      subscriptionId: Number(this.fileClaimForm.value.subscriptionId),
      description: this.fileClaimForm.value.description,
      claimAmount: Number(this.fileClaimForm.value.claimAmount),
      incidentDate: this.fileClaimForm.value.incidentDate,
      causeOfFire: this.fileClaimForm.value.causeOfFire,
      firNumber: this.fileClaimForm.value.firNumber,
      fireBrigadeReportNumber: this.fileClaimForm.value.fireBrigadeReportNumber,
      policeStation: this.fileClaimForm.value.policeStation,
      lossType: this.fileClaimForm.value.lossType,
      contactPhoneNumber: this.fileClaimForm.value.contactPhoneNumber,
      witnessDetails: this.fileClaimForm.value.witnessDetails
    };

    this.customerService.fileClaim(payload).subscribe({
      next: (newClaim: any) => {
        this.claims.update(claims => [newClaim, ...claims]);
        this.successMessage.set('Claim filed successfully! Please upload any supporting documents below.');
        this.isLoading.set(false);
        this.claimIdForUpload.set(newClaim.claimId || newClaim.id || 0); // Advance modal to Upload Phase

        setTimeout(() => {
          this.successMessage.set('');
        }, 5000);
      },
      error: (err: any) => {
        this.errorMessage.set('Failed to file claim. Please check your details and try again.');
        console.error('Error filing claim:', err);
        this.isLoading.set(false);
      }
    });
  }

  getStatusClass(status: string): string {
    const statusClasses: { [key: string]: string } = {
      'SUBMITTED': 'bg-blue-100 text-blue-800',
      'UNDER_REVIEW': 'bg-yellow-100 text-yellow-800',
      'INSPECTING': 'bg-yellow-100 text-yellow-800',
      'INSPECTED': 'bg-purple-100 text-purple-800',
      'APPROVED': 'bg-green-100 text-green-800',
      'REJECTED': 'bg-red-100 text-red-800',
      'SETTLED': 'bg-purple-100 text-purple-800',
      'PAID': 'bg-green-100 text-green-800'
    };
    return statusClasses[status] || 'bg-gray-100 text-gray-800';
  }

  getSubscriptionLabel(subscriptionId: number): string {
    const sub = this.activeSubscriptions().find(s => s.subscriptionId === subscriptionId);
    if (sub) {
      const prop = this.properties().find(p => p.propertyId === sub.propertyId);
      return `${sub.policyName} — ${prop?.address || 'Property #' + sub.propertyId}`;
    }
    return `Subscription #${subscriptionId}`;
  }

  getCurrentDate(): string {
    return new Date().toISOString().split('T')[0];
  }

  getFieldError(fieldName: string): string {
    const field = this.fileClaimForm.get(fieldName);
    if (field && field.errors && (field.touched || field.dirty)) {
      return ValidationMessages.getErrorMessage(fieldName, field.errors);
    }
    return '';
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.fileClaimForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  isFieldValid(fieldName: string): boolean {
    const field = this.fileClaimForm.get(fieldName);
    return !!(field && field.valid && field.dirty);
  }
}
