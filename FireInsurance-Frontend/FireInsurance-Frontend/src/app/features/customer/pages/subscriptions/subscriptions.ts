import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CustomerService } from '../../services/customer.service';
import { CreateEndorsementRequest, Endorsement, PremiumBreakdown, Subscription } from '../../../../core/models/customer.model';
import { Property } from '../../../../core/models/property.model';
import { Policy } from '../../../../core/models/policy.model';
import { CustomValidators } from '../../../../shared/validators/custom-validators';
import { ValidationMessages } from '../../../../shared/helpers/validation-messages';

@Component({
  selector: 'app-subscriptions',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './subscriptions.html',
  styleUrls: ['./subscriptions.css']
})
export class SubscriptionsComponent implements OnInit {
  private readonly customerService = inject(CustomerService);
  private readonly fb = inject(FormBuilder);

  subscriptions = signal<Subscription[]>([]); // Active
  pendingSubscriptions = signal<Subscription[]>([]); // Pending
  properties = signal<Property[]>([]);
  availablePolicies = signal<Policy[]>([]);
  
  isLoading = signal(false);
  isPaying = signal(false);
  isSubmittingEndorsement = signal(false);
  errorMessage = signal('');
  successMessage = signal('');
  showSubscribeModal = signal(false);
  showEndorsementModal = signal(false);
  showPremiumBreakdownModal = signal(false);
  isLoadingPremiumBreakdown = signal(false);
  selectedEndorsementSubscriptionId = signal<number | null>(null);
  selectedPremiumSubscription = signal<Subscription | null>(null);
  premiumBreakdown = signal<PremiumBreakdown | null>(null);
  endorsementsBySubscription = signal<Record<number, Endorsement[]>>({});

  subscribeForm: FormGroup = this.fb.group({
    propertyId: [0, [Validators.required, Validators.min(1), CustomValidators.positiveNumber()]],
    policyId: [0, [Validators.required, Validators.min(1), CustomValidators.positiveNumber()]]
  });

  endorsementForm: FormGroup = this.fb.group({
    changeType: ['Coverage Change', [Validators.required]],
    requestedCoverage: [null],
    newOccupancyType: [''],
    newHazardousGoods: [''],
    reason: ['', [Validators.required, Validators.minLength(8)]]
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    
    // Load subscriptions, and properties in parallel
    Promise.all([
      this.customerService.getMySubscriptions().toPromise(),
      this.customerService.getMyProperties().toPromise()
    ]).then(([subs, properties]) => {
      subs = subs || [];
      
      // Deduplicate subscriptions: only keep the most recent one for each property-policy combination
      const uniqueSubs = new Map<string, Subscription>();
      
      // Sort ascending, so the last one remaining in the map is the latest
      const sortedSubs = [...subs].sort((a, b) => (a.subscriptionId || 0) - (b.subscriptionId || 0));
      
      sortedSubs.forEach(s => {
        const key = `${s.propertyId}-${s.policyName}`;
        uniqueSubs.set(key, s);
      });
      
      const distinctSubs = Array.from(uniqueSubs.values());

      const activeSubscriptions = distinctSubs.filter(s => s.status === 'ACTIVE');
      this.subscriptions.set(activeSubscriptions);
      this.pendingSubscriptions.set(distinctSubs.filter(s => s.status !== 'ACTIVE'));
      this.properties.set(properties || []);
      this.loadEndorsementsForSubscriptions(activeSubscriptions);
      this.isLoading.set(false);
    }).catch(err => {
      this.errorMessage.set('Failed to load data');
      console.error('Error loading data:', err);
      this.isLoading.set(false);
    });
  }

  openSubscribeModal(): void {
    this.showSubscribeModal.set(true);
    this.loadAvailablePolicies();
    this.subscribeForm.reset({
      propertyId: 0,
      policyId: 0
    });
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  closeSubscribeModal(): void {
    this.showSubscribeModal.set(false);
    this.subscribeForm.reset();
  }

  openEndorsementModal(subscriptionId: number): void {
    this.selectedEndorsementSubscriptionId.set(subscriptionId);
    this.showEndorsementModal.set(true);
    this.endorsementForm.reset({
      changeType: 'Coverage Change',
      requestedCoverage: null,
      newOccupancyType: '',
      newHazardousGoods: '',
      reason: ''
    });
  }

  closeEndorsementModal(): void {
    this.showEndorsementModal.set(false);
    this.selectedEndorsementSubscriptionId.set(null);
  }

  openPremiumBreakdown(subscription: Subscription): void {
    if (!subscription.subscriptionId || !subscription.premiumAmount) {
      return;
    }

    this.selectedPremiumSubscription.set(subscription);
    this.showPremiumBreakdownModal.set(true);
    this.isLoadingPremiumBreakdown.set(true);
    this.premiumBreakdown.set(null);
    this.errorMessage.set('');

    this.customerService.getPremiumBreakdown(subscription.subscriptionId).subscribe({
      next: (breakdown) => {
        this.premiumBreakdown.set(breakdown);
        this.isLoadingPremiumBreakdown.set(false);
      },
      error: (err) => {
        console.error('Premium breakdown loading failed:', err);
        this.errorMessage.set(err?.error?.message || 'Unable to load premium breakdown right now.');
        this.isLoadingPremiumBreakdown.set(false);
      }
    });
  }

  closePremiumBreakdownModal(): void {
    this.showPremiumBreakdownModal.set(false);
    this.isLoadingPremiumBreakdown.set(false);
    this.selectedPremiumSubscription.set(null);
    this.premiumBreakdown.set(null);
  }

  submitEndorsement(): void {
    const subscriptionId = this.selectedEndorsementSubscriptionId();
    if (!subscriptionId) {
      return;
    }
    if (this.endorsementForm.invalid) {
      this.endorsementForm.markAllAsTouched();
      return;
    }

    this.isSubmittingEndorsement.set(true);
    const payload: CreateEndorsementRequest = {
      subscriptionId,
      changeType: this.endorsementForm.value.changeType,
      requestedCoverage: this.endorsementForm.value.requestedCoverage || undefined,
      newOccupancyType: this.endorsementForm.value.newOccupancyType || undefined,
      newHazardousGoods: this.endorsementForm.value.newHazardousGoods || undefined,
      reason: this.endorsementForm.value.reason
    };

    this.customerService.createEndorsement(payload).subscribe({
      next: (endorsement) => {
        this.endorsementsBySubscription.update(map => {
          const existing = map[subscriptionId] || [];
          return { ...map, [subscriptionId]: [endorsement, ...existing] };
        });
        this.isSubmittingEndorsement.set(false);
        this.closeEndorsementModal();
        this.successMessage.set('Endorsement request submitted for review.');
        setTimeout(() => this.successMessage.set(''), 4000);
      },
      error: (err) => {
        console.error('Endorsement request failed:', err);
        this.errorMessage.set(err?.error?.message || 'Could not submit endorsement request.');
        this.isSubmittingEndorsement.set(false);
      }
    });
  }

  getEndorsements(subscriptionId: number): Endorsement[] {
    return this.endorsementsBySubscription()[subscriptionId] || [];
  }

  private loadEndorsementsForSubscriptions(activeSubscriptions: Subscription[]): void {
    const nextMap: Record<number, Endorsement[]> = {};
    activeSubscriptions.forEach((subscription) => {
      this.customerService.getSubscriptionEndorsements(subscription.subscriptionId).subscribe({
        next: (endorsements) => {
          nextMap[subscription.subscriptionId] = endorsements || [];
          this.endorsementsBySubscription.set({ ...nextMap });
        },
        error: () => {
          nextMap[subscription.subscriptionId] = [];
          this.endorsementsBySubscription.set({ ...nextMap });
        }
      });
    });
  }

  loadAvailablePolicies(): void {
    // GET /api/policies — accessible to CUSTOMER role
    this.customerService.getMyPolicies().subscribe({
      next: (policies) => {
        this.availablePolicies.set(policies);
      },
      error: (err) => {
        console.error('Error loading available policies:', err);
      }
    });
  }

  subscribeToPolicy(): void {
    if (this.subscribeForm.invalid) {
      this.subscribeForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');

    // HTML <select> always returns string values — explicitly cast to numbers
    // so the backend Java DTO receives Long types (not strings), preventing 400 errors
    const payload = {
      propertyId: Number(this.subscribeForm.value.propertyId),
      policyId: Number(this.subscribeForm.value.policyId)
    };
    
    this.customerService.subscribeToPolicy(payload).subscribe({
      next: (newSubscription) => {
        // Remove any older pending subscription for the same property/policy before pushing the new one
        this.pendingSubscriptions.update(subs => {
          const filtered = subs.filter(s => !(s.propertyId === newSubscription.propertyId && s.policyName === newSubscription.policyName));
          return [...filtered, newSubscription];
        });
        
        this.successMessage.set('Subscription request submitted successfully!');
        this.isLoading.set(false);
        this.closeSubscribeModal();
        
        setTimeout(() => {
          this.successMessage.set('');
        }, 3000);
      },
      error: (err) => {
        this.errorMessage.set('Failed to submit subscription request');
        console.error('Error subscribing to policy:', err);
        this.isLoading.set(false);
      }
    });
  }

  getStatusClass(status: string): string {
    const statusClasses: { [key: string]: string } = {
      'SUBMITTED': 'status-pending',
      'PENDING': 'status-pending',
      'INSPECTION_PENDING': 'status-pending',
      'UNDER_REVIEW': 'status-pending',
      'APPROVED': 'status-approved',
      'PAYMENT_PENDING': 'status-pending',
      'REJECTED': 'status-rejected',
      'ACTIVE': 'status-active',
      'EXPIRED': 'status-expired',
      'CANCELLED': 'status-cancelled'
    };
    return statusClasses[status] || 'status-default';
  }

  getPropertyAddress(propertyId: number): string {
    const property = this.properties().find(p => p.propertyId === propertyId);
    return property?.address || 'Unknown';
  }

  getFieldError(fieldName: string): string {
    const field = this.subscribeForm.get(fieldName);
    if (field && field.invalid && (field.dirty || field.touched)) {
      return ValidationMessages.getErrorMessage(fieldName, field.errors);
    }
    return '';
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.subscribeForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  isFieldValid(fieldName: string): boolean {
    const field = this.subscribeForm.get(fieldName);
    return !!(field && field.valid && field.dirty);
  }

  getRiskLabel(riskScore: number): string {
    if (riskScore <= 2) return 'Low';
    if (riskScore <= 4) return 'Normal';
    if (riskScore <= 6) return 'Moderate';
    if (riskScore <= 8) return 'High';
    return 'Very High';
  }

  getRiskClass(riskScore: number): string {
    if (riskScore <= 2) return 'bg-green-100 text-green-800';
    if (riskScore <= 4) return 'bg-blue-100 text-blue-800';
    if (riskScore <= 6) return 'bg-yellow-100 text-yellow-800';
    if (riskScore <= 8) return 'bg-orange-100 text-orange-800';
    return 'bg-red-100 text-red-800';
  }

  canViewPremiumBreakdown(subscription: Subscription): boolean {
    return !!subscription.subscriptionId && !!subscription.premiumAmount && subscription.premiumAmount > 0;
  }

  getMonthlyInstallment(subscription: Subscription): number | null {
    const breakdown = this.premiumBreakdown();
    if (breakdown && this.selectedPremiumSubscription()?.subscriptionId === subscription.subscriptionId) {
      return breakdown.monthlyPremium;
    }

    if (!subscription.premiumAmount || !subscription.startDate || !subscription.endDate) {
      return null;
    }

    const start = new Date(subscription.startDate);
    const end = new Date(subscription.endDate);
    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || end <= start) {
      return null;
    }

    const monthDiff = Math.max(1, Math.round((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24 * 30.44)));
    return Math.round((subscription.premiumAmount / monthDiff) * 100) / 100;
  }

  getBreakdownAmountClass(kind: string): string {
    if (kind === 'DISCOUNT') {
      return 'text-green-700';
    }
    if (kind === 'ADJUSTMENT') {
      return 'text-fire-red';
    }
    return 'text-gray-900';
  }

  downloadCoverNote(subscription: Subscription): void {
    if (!subscription.subscriptionId) {
      return;
    }
    this.customerService.downloadCoverNote(subscription.subscriptionId).subscribe({
      next: (blob) => {
        this.triggerDownload(blob, subscription.coverNoteFileName || `CoverNote-SUB${subscription.subscriptionId}.txt`);
      },
      error: (err) => {
        console.error('Cover note download failed:', err);
        this.errorMessage.set('Unable to download cover note right now.');
      }
    });
  }

  downloadPolicyDocument(subscription: Subscription): void {
    if (!subscription.subscriptionId) {
      return;
    }
    this.customerService.downloadPolicyDocument(subscription.subscriptionId).subscribe({
      next: (blob) => {
        this.triggerDownload(blob, subscription.policyDocumentFileName || `PolicyDocument-SUB${subscription.subscriptionId}.txt`);
      },
      error: (err) => {
        console.error('Policy document download failed:', err);
        this.errorMessage.set('Unable to download policy document right now.');
      }
    });
  }

  private triggerDownload(blob: Blob, fileName: string): void {
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = fileName;
    anchor.click();
    window.URL.revokeObjectURL(url);
  }

  payNow(subscriptionId: number): void {
    this.isPaying.set(true);
    this.errorMessage.set('');
    this.customerService.payForSubscription(subscriptionId).subscribe({
      next: () => {
        this.successMessage.set('Payment accepted. Policy is now ACTIVE.');
        this.isPaying.set(false);
        this.loadData();
        setTimeout(() => this.successMessage.set(''), 4000);
      },
      error: (err) => {
        console.error('Payment failed:', err);
        this.errorMessage.set(err?.error?.message || 'Payment failed. Please try again.');
        this.isPaying.set(false);
      }
    });
  }
}
