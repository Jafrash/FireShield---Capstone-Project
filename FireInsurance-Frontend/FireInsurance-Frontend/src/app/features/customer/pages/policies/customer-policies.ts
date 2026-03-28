import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CustomerService } from '../../services/customer.service';
import { Policy } from '../../../../core/models/policy.model';
import { Property } from '../../../../core/models/property.model';
import { Subscription, SubscriptionRequest } from '../../../../core/models/customer.model';
import { CustomValidators } from '../../../../shared/validators/custom-validators';
import { ValidationMessages } from '../../../../shared/helpers/validation-messages';
import { DocumentUploadComponent } from '../../../../shared/components/ui/document-upload/document-upload.component';

@Component({
  selector: 'app-customer-policies',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule, DocumentUploadComponent],
  templateUrl: './customer-policies.html',
  styleUrls: ['./customer-policies.css']
})
export class CustomerPoliciesComponent implements OnInit {
  private customerService = inject(CustomerService);
  private fb = inject(FormBuilder);
  private router = inject(Router);

  // State using signals
  policies = signal<Policy[]>([]);
  properties = signal<Property[]>([]);
  subscriptions = signal<Subscription[]>([]);
  selectedPolicy = signal<Policy | null>(null);
  
  isLoading = signal(false);
  isLoadingProperties = signal(false);
  isSubmitting = signal(false);
  showApplyModal = signal(false);
  showDetailsModal = signal(false);
  isUploadStep = signal(false);
  newSubscriptionId = signal<number | null>(null);
  propertyIdForUpload = signal<number | null>(null);

  // 3-step wizard state
  currentStep = signal<1 | 2 | 3>(1);
  hasLossHistory = signal(false);
  hasDeclinedBefore = signal(false);
  declarationAccepted = signal(false);
  
  errorMessage = signal('');
  successMessage = signal('');

  // Application form
  applicationForm: FormGroup = this.fb.group({
    propertyId: [null, [Validators.required, CustomValidators.positiveNumber()]],
    requestedCoverage: [null, [Validators.required, Validators.min(1), CustomValidators.positiveNumber()]],
    constructionType: ['', Validators.required],
    roofType: ['', Validators.required],
    numberOfFloors: [null, [Validators.required, Validators.min(1)]],
    occupancyType: ['', Validators.required],
    manufacturingProcess: [''],
    hazardousGoods: [''],
    previousLossHistory: [''],
    insuranceDeclinedBefore: [false],
    propertyValue: [null, [Validators.required, Validators.min(1)]]
  });

  // Computed signals
  getSubscriptionStatus = computed(() => {
    const subs = this.subscriptions();
    return (policyId: number) => {
      return subs.find(sub => sub.policyId === policyId);
    };
  });

  isIndustrialOrCommercial = computed(() => {
    const occ = this.applicationForm.get('occupancyType')?.value;
    return occ === 'COMMERCIAL' || occ === 'INDUSTRIAL';
  });

  ngOnInit(): void {
    this.loadPolicies();
    this.loadProperties();
    this.loadSubscriptions();
  }

  loadPolicies(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    
    this.customerService.getAllPolicies().subscribe({
      next: (data) => {
        this.policies.set(data);
        this.isLoading.set(false);
      },
      error: (err: Error) => {
        console.error('Error loading policies:', err);
        this.errorMessage.set('Unable to load policies. Please try again.');
        this.isLoading.set(false);
      }
    });
  }

  loadProperties(): void {
    this.customerService.getMyProperties().subscribe({
      next: (data) => {
        this.properties.set(data);
      },
      error: (err: Error) => {
        console.error('Error loading properties:', err);
      }
    });
  }

  loadSubscriptions(): void {
    this.customerService.getMySubscriptions().subscribe({
      next: (data) => {
        const subs = data || [];
        // Deduplicate subscriptions: only keep the most recent one for each property-policy combination
        const uniqueSubs = new Map<string, Subscription>();
        
        // Sort ascending, so the last one remaining in the map is the latest
        const sortedSubs = [...subs].sort((a, b) => (a.subscriptionId || 0) - (b.subscriptionId || 0));
        
        sortedSubs.forEach(s => {
          const key = `${s.propertyId}-${s.policyName}`;
          uniqueSubs.set(key, s);
        });
        
        this.subscriptions.set(Array.from(uniqueSubs.values()));
      },
      error: (err: Error) => {
        console.error('Error loading subscriptions:', err);
      }
    });
  }

  viewDetails(policy: Policy): void {
    this.selectedPolicy.set(policy);
    this.showDetailsModal.set(true);
  }

  closeDetailsModal(): void {
    this.showDetailsModal.set(false);
    this.selectedPolicy.set(null);
  }

  navigateToDetails(policyId: number): void {
    this.router.navigate(['/customer/policies', policyId]);
  }

  openApplyModal(policy: Policy): void {
    if (this.properties().length === 0) {
      this.errorMessage.set('Please add a property first before applying for a policy.');
      setTimeout(() => this.errorMessage.set(''), 5000);
      return;
    }

    this.selectedPolicy.set(policy);
    this.showApplyModal.set(true);
    this.applicationForm.reset();
    this.currentStep.set(1);
    this.hasLossHistory.set(false);
    this.hasDeclinedBefore.set(false);
    this.declarationAccepted.set(false);

    // Set max coverage validation
    this.applicationForm.get('requestedCoverage')?.setValidators([
      Validators.required,
      Validators.min(1),
      Validators.max(policy.maxCoverageAmount)
    ]);
  }

  closeApplyModal(): void {
    this.showApplyModal.set(false);
    this.selectedPolicy.set(null);
    this.applicationForm.reset();
    this.errorMessage.set('');
    this.isUploadStep.set(false);
    this.newSubscriptionId.set(null);
    this.propertyIdForUpload.set(null);
    this.successMessage.set('');
    this.currentStep.set(1);
    this.hasLossHistory.set(false);
    this.hasDeclinedBefore.set(false);
    this.declarationAccepted.set(false);
  }

  nextStep(): void {
    if (this.currentStep() === 1) {
      const s1Controls = ['propertyId', 'requestedCoverage'];
      s1Controls.forEach(c => this.applicationForm.get(c)?.markAsTouched());
      const step1Valid = s1Controls.every(c => this.applicationForm.get(c)?.valid);
      if (step1Valid) this.currentStep.set(2);
    } else if (this.currentStep() === 2) {
      const s2Controls = ['constructionType', 'roofType', 'numberOfFloors', 'occupancyType', 'propertyValue'];
      s2Controls.forEach(c => this.applicationForm.get(c)?.markAsTouched());
      const step2Valid = s2Controls.every(c => this.applicationForm.get(c)?.valid);
      if (step2Valid) this.currentStep.set(3);
    }
  }

  prevStep(): void {
    if (this.currentStep() > 1) {
      this.currentStep.set((this.currentStep() - 1) as 1 | 2 | 3);
    }
  }

  setLossHistory(val: boolean): void {
    this.hasLossHistory.set(val);
    if (!val) this.applicationForm.get('previousLossHistory')?.setValue('');
  }

  setDeclinedBefore(val: boolean): void {
    this.hasDeclinedBefore.set(val);
    this.applicationForm.get('insuranceDeclinedBefore')?.setValue(val);
  }

  setCoverage(amount: number): void {
    this.applicationForm.get('requestedCoverage')?.setValue(amount);
  }

  submitApplication(): void {
    if (this.applicationForm.invalid) {
      this.applicationForm.markAllAsTouched();
      return;
    }

    const policy = this.selectedPolicy();
    if (!policy) return;

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    const request = {
      policyId: policy.policyId,
      propertyId: this.applicationForm.value.propertyId,
      requestedCoverage: this.applicationForm.value.requestedCoverage,
      constructionType: this.applicationForm.value.constructionType || null,
      roofType: this.applicationForm.value.roofType || null,
      numberOfFloors: this.applicationForm.value.numberOfFloors || null,
      occupancyType: this.applicationForm.value.occupancyType || null,
      manufacturingProcess: this.applicationForm.value.manufacturingProcess || null,
      hazardousGoods: this.applicationForm.value.hazardousGoods || null,
      previousLossHistory: this.hasLossHistory() ? (this.applicationForm.value.previousLossHistory || null) : null,
      insuranceDeclinedBefore: this.hasDeclinedBefore(),
      propertyValue: this.applicationForm.value.propertyValue || null,
      declarationAccepted: true
    };

    this.customerService.subscribeToPolicy(request).subscribe({
      next: (newSub) => {
        this.isSubmitting.set(false);
        this.newSubscriptionId.set(newSub.subscriptionId || (newSub as any).id);
        this.propertyIdForUpload.set(request.propertyId);
        this.isUploadStep.set(true);
        this.successMessage.set('Policy requested! Please upload the required documents below to complete your application.');
        this.loadSubscriptions();
        
        // Clear success message after 8 seconds
        setTimeout(() => this.successMessage.set(''), 8000);
      },
      error: (err: Error) => {
        console.error('Error submitting application:', err);
        this.errorMessage.set('Failed to submit application. Please try again.');
        this.isSubmitting.set(false);
      }
    });
  }

  getPolicyTypeColor(type?: string): string {
    const colors: { [key: string]: string } = {
      'Residential': 'bg-blue-100 text-blue-800',
      'Commercial': 'bg-purple-100 text-purple-800',
      'Industrial': 'bg-orange-100 text-orange-800',
      'Multi-Family': 'bg-green-100 text-green-800',
      'Wildfire': 'bg-red-100 text-red-800'
    };
    return colors[type || ''] || 'bg-gray-100 text-gray-800';
  }

  getStatusColor(status: string): string {
    const colors: { [key: string]: string } = {
      'PENDING': 'bg-yellow-100 text-yellow-800',
      'ACTIVE': 'bg-green-100 text-green-800',
      'APPROVED': 'bg-green-100 text-green-800',
      'REJECTED': 'bg-red-100 text-red-800',
      'CANCELLED': 'bg-gray-100 text-gray-800',
      'EXPIRED': 'bg-gray-100 text-gray-800'
    };
    return colors[status] || 'bg-gray-100 text-gray-800';
  }

  canApplyForPolicy(policyId: number): boolean {
    const subscription = this.getSubscriptionStatus()(policyId);
    if (!subscription) return true;
    
    // Can reapply if rejected, cancelled, or expired
    return ['REJECTED', 'CANCELLED', 'EXPIRED'].includes(subscription.status);
  }

  getApplyButtonText(policyId: number): string {
    const subscription = this.getSubscriptionStatus()(policyId);
    if (!subscription) return 'Apply Now';
    
    if (['REJECTED', 'CANCELLED', 'EXPIRED'].includes(subscription.status)) {
      return 'Reapply';
    }
    return 'Applied';
  }

  formatCurrency(amount: number): string {
    return amount.toLocaleString('en-IN');
  }

  getDurationText(months: number): string {
    if (months < 12) return `${months} months`;
    const years = Math.floor(months / 12);
    const remainingMonths = months % 12;
    if (remainingMonths === 0) return `${years} ${years === 1 ? 'year' : 'years'}`;
    return `${years} ${years === 1 ? 'year' : 'years'} ${remainingMonths} months`;
  }

  /**
   * Calculate monthly premium from annual premium
   */
  getMonthlyPremium(annualPremium: number): number {
    return annualPremium / 12;
  }

  /**
   * Calculate estimated premium for requested coverage amount
   * Formula: (Requested Coverage / Max Coverage) * Base Premium * Risk Factor
   * This gives customers an estimate before applying
   */
  calculateEstimatedPremium(policy: Policy, requestedCoverage: number): number {
    const coverageRatio = requestedCoverage / policy.maxCoverageAmount;
    const riskFactor = policy.premiumCalculationFactors?.riskMultiplier || 1.2;
    const locationFactor = policy.premiumCalculationFactors?.locationFactor || 1.1;
    const constructionFactor = policy.premiumCalculationFactors?.constructionFactor || 1.0;
    
    // Base calculation: proportional to coverage requested
    const basePremiumAmount = policy.basePremium * coverageRatio;
    
    // Apply risk factors
    const totalPremium = basePremiumAmount * riskFactor * locationFactor * constructionFactor;
    
    return Math.round(totalPremium * 100) / 100; // Round to 2 decimals
  }

  /**
   * Calculate claim settlement amount
   * Takes into account deductible and claim settlement ratio
   */
  calculateClaimSettlement(policy: Policy, claimAmount: number): number {
    const deductible = policy.deductible || 0;
    const claimSettlementRatio = (policy.claimSettlementRatio || 100) / 100;
    
    // Amount after deductible
    const amountAfterDeductible = Math.max(0, claimAmount - deductible);
    
    // Apply settlement ratio
    const settlementAmount = amountAfterDeductible * claimSettlementRatio;
    
    return Math.round(settlementAmount);
  }

  /**
   * Get premium breakdown for transparency
   */
  getPremiumBreakdown(policy: Policy, requestedCoverage: number): {
    basePremium: number;
    riskCharge: number;
    locationCharge: number;
    constructionCharge: number;
    gst: number;
    totalPremium: number;
  } {
    const coverageRatio = requestedCoverage / policy.maxCoverageAmount;
    const basePremium = policy.basePremium * coverageRatio;
    
    const riskMultiplier = policy.premiumCalculationFactors?.riskMultiplier || 1.2;
    const locationFactor = policy.premiumCalculationFactors?.locationFactor || 1.1;
    const constructionFactor = policy.premiumCalculationFactors?.constructionFactor || 1.0;
    
    const riskCharge = basePremium * (riskMultiplier - 1);
    const locationCharge = basePremium * (locationFactor - 1);
    const constructionCharge = basePremium * (constructionFactor - 1);
    
    const subtotal = basePremium + riskCharge + locationCharge + constructionCharge;
    const gst = subtotal * 0.18; // 18% GST in India
    const totalPremium = subtotal + gst;
    
    return {
      basePremium: Math.round(basePremium),
      riskCharge: Math.round(riskCharge),
      locationCharge: Math.round(locationCharge),
      constructionCharge: Math.round(constructionCharge),
      gst: Math.round(gst),
      totalPremium: Math.round(totalPremium)
    };
  }

  /**
   * Calculate maturity benefit (for certain policy types)
   * For fire insurance with investment component
   */
  getMaturityBenefit(policy: Policy, premiumPaid: number, yearsCompleted: number): number {
    // Simple bonus calculation: 5% compound interest per year
    const annualRate = 0.05;
    const maturityAmount = premiumPaid * Math.pow(1 + annualRate, yearsCompleted);
    
    return Math.round(maturityAmount);
  }

  /**
   * Get Sum Insured recommendations based on policy
   */
  getSumInsuredRecommendations(policy: Policy): {
    minimum: number;
    recommended: number;
    maximum: number;
  } {
    return {
      minimum: policy.sumInsuredMin || policy.maxCoverageAmount * 0.25,
      recommended: policy.maxCoverageAmount * 0.6, // 60% of max as recommended
      maximum: policy.maxCoverageAmount
    };
  }

  /**
   * Calculate GST on premium (18% as per Indian insurance regulations)
   */
  getGSTAmount(policy: Policy): number {
    return Math.round(policy.basePremium * 0.18);
  }

  /**
   * Calculate total annual premium including GST
   */
  getTotalAnnualPremium(policy: Policy): number {
    return policy.basePremium + this.getGSTAmount(policy);
  }

  /**
   * Calculate premium rate per ₹1000 of coverage
   * This helps customers understand the cost efficiency
   */
  getPremiumRate(policy: Policy): string {
    const rate = (policy.basePremium / policy.maxCoverageAmount) * 1000;
    return rate.toFixed(2);
  }

  /**
   * Calculate claim settlement timeline based on settlement ratio
   * Higher settlement ratio indicates faster processing
   */
  getClaimSettlementDays(policy: Policy): string {
    const ratio = policy.claimSettlementRatio || 95;
    if (ratio >= 95) return '15-20 days';
    if (ratio >= 90) return '20-25 days';
    return '25-30 days';
  }

  /**
   * Get risk category based on coverage amount for better customer understanding
   */
  getRiskCategory(maxCoverage: number): string {
    if (maxCoverage <= 1000000) return 'Standard';
    if (maxCoverage <= 5000000) return 'Enhanced';
    if (maxCoverage <= 10000000) return 'Premium';
    return 'Elite';
  }

  /**
   * Renew a policy subscription
   * Creates a new subscription with continuity and applies NCB discount
   */
  renewPolicy(subscription: Subscription): void {
    if (!subscription.renewalEligible) {
      this.errorMessage.set('This policy is not eligible for renewal yet.');
      setTimeout(() => this.errorMessage.set(''), 5000);
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');
    
    this.customerService.renewPolicy(subscription.subscriptionId).subscribe({
      next: (renewedSub) => {
        this.isSubmitting.set(false);
        this.successMessage.set(
          `Policy renewed successfully! ${renewedSub.ncbDiscount ? `No Claim Bonus of ${(renewedSub.ncbDiscount * 100).toFixed(0)}% applied.` : ''}`
        );
        this.loadSubscriptions();
        
        // Clear success message after 8 seconds
        setTimeout(() => this.successMessage.set(''), 8000);
      },
      error: (err: Error) => {
        console.error('Error renewing policy:', err);
        this.errorMessage.set('Failed to renew policy. Please try again or contact support.');
        this.isSubmitting.set(false);
      }
    });
  }

  /**
   * Get NCB discount badge color based on claim-free years
   */
  getNCBBadgeColor(years?: number): string {
    if (!years || years === 0) return 'bg-gray-100 text-gray-600';
    if (years === 1) return 'bg-blue-100 text-blue-700';
    if (years === 2) return 'bg-green-100 text-green-700';
    if (years === 3) return 'bg-yellow-100 text-yellow-700';
    if (years === 4) return 'bg-orange-100 text-orange-700';
    return 'bg-purple-100 text-purple-700'; // 5+ years
  }

  /**
   * Format NCB discount as percentage
   */
  getNCBDiscountText(discount?: number): string {
    if (!discount || discount === 0) return 'No discount';
    return `${(discount * 100).toFixed(0)}% discount`;
  }

  /**
   * Check if subscription is expiring soon (within 30 days)
   */
  isExpiringSoon(subscription: Subscription): boolean {
    return subscription.renewalEligible === true || (subscription.daysRemaining !== undefined && subscription.daysRemaining <= 30);
  }

  /**
   * Get days remaining text with color coding
   */
  getDaysRemainingText(subscription: Subscription): { text: string; color: string } {
    const days = subscription.daysRemaining;
    if (days === undefined || days === null) {
      return { text: 'N/A', color: 'text-gray-500' };
    }
    
    if (days <= 0) {
      return { text: 'Expired', color: 'text-red-600 font-bold' };
    } else if (days <= 7) {
      return { text: `${days} ${days === 1 ? 'day' : 'days'} left`, color: 'text-red-600 font-bold' };
    } else if (days <= 30) {
      return { text: `${days} days left`, color: 'text-orange-600 font-semibold' };
    } else if (days <= 60) {
      return { text: `${days} days left`, color: 'text-yellow-600' };
    }
    return { text: `${days} days left`, color: 'text-green-600' };
  }

  getErrorMessage(controlName: string): string {
    const field = this.applicationForm.get(controlName);
    if (field && field.invalid && (field.dirty || field.touched)) {
      return ValidationMessages.getErrorMessage(controlName, field.errors);
    }
    return '';
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.applicationForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  isFieldValid(fieldName: string): boolean {
    const field = this.applicationForm.get(fieldName);
    return !!(field && field.valid && field.dirty);
  }
}
