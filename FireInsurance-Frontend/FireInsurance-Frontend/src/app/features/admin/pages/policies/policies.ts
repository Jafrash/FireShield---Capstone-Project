import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AdminService } from '../../../../features/admin/services/admin.service';
import { Policy } from '../../../../core/models/policy.model';
import { CustomValidators } from '../../../../shared/validators/custom-validators';
import { ValidationMessages } from '../../../../shared/helpers/validation-messages';

interface PolicyTemplate {
  name: string;
  type: string;
  description: string;
  coverageDetails: string;
  detailedDescription: string;
  basePremium: number;
  maxCoverageAmount: number;
  durationMonths: number;
  risksCovered: string[];
  propertyTypesEligible: string[];
  termsAndConditions: string;
  icon: string;
  color: string;
}

@Component({
  selector: 'app-policies',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './policies.html'
})
export class PoliciesComponent implements OnInit {
  private adminService = inject(AdminService);
  
  policies = signal<Policy[]>([]);
  filteredPolicies = signal<Policy[]>([]);
  isLoading = signal<boolean>(true);
  errorMessage = signal<string>('');
  searchTerm = signal<string>('');
  
  showAddModal = signal<boolean>(false);
  showTemplatesModal = signal<boolean>(false);
  policyForm: FormGroup;

  policyTemplates: PolicyTemplate[] = [
    {
      name: 'Standard Fire and Special Perils Policy (SFSP)',
      type: 'RESIDENTIAL',
      description: 'Most common fire insurance for homes, factories, shops, and warehouses',
      coverageDetails: 'Comprehensive protection against fire, lightning, explosion, natural disasters, and malicious damage',
      detailedDescription: 'The Standard Fire and Special Perils Policy is ideal for residential and commercial properties. It covers damages caused by fire, lightning, explosions, storms, floods, earthquakes (optional), riots, strikes, and malicious damage. This policy is widely used for homes, offices, factories, shops, and warehouses.',
      basePremium: 10000,
      maxCoverageAmount: 5000000,
      durationMonths: 12,
      risksCovered: ['Fire', 'Lightning', 'Explosion/Implosion', 'Storm/Cyclone/Hurricane', 'Flood and Inundation', 'Earthquake (Optional)', 'Riot/Strike/Malicious Damage'],
      propertyTypesEligible: ['Residential Buildings', 'Commercial Offices', 'Factories', 'Shops', 'Warehouses'],
      termsAndConditions: 'Premium rate: 0.1% - 0.3% of property value per year. Earthquake coverage is optional with additional premium. Claims settled based on actual damage assessment by surveyor.',
      icon: 'home_work',
      color: 'blue'
    },
    {
      name: 'Valued Policy',
      type: 'COMMERCIAL',
      description: 'For items with difficult-to-determine value like art, antiques, and rare collections',
      coverageDetails: 'Fixed value coverage for artworks, antiques, and unique collectibles',
      detailedDescription: 'Valued Policy is designed for items whose exact market value is difficult to determine, such as artworks, antiques, rare manuscripts, and precious collections. The value is predetermined and agreed upon at the time of policy purchase. In case of total loss due to fire, the full agreed value is paid without depreciation.',
      basePremium: 25000,
      maxCoverageAmount: 10000000,
      durationMonths: 12,
      risksCovered: ['Fire', 'Lightning', 'Explosion', 'Theft (Optional)'],
      propertyTypesEligible: ['Art Galleries', 'Museums', 'Antique Shops', 'Private Collections', 'Heritage Properties'],
      termsAndConditions: 'Value must be certified by approved valuers. Premium is 0.2% - 0.5% of agreed value. Full agreed value is paid upon total loss. Partial damage assessed by expert valuers.',
      icon: 'museum',
      color: 'purple'
    },
    {
      name: 'Specific Policy',
      type: 'RESIDENTIAL',
      description: 'Covers specific property for a fixed amount - cheaper but with under-insurance risk',
      coverageDetails: 'Fixed coverage amount for specific property at lower premium',
      detailedDescription: 'Specific Policy provides coverage for a specific property up to a predetermined fixed amount, which may be less than the actual property value. This policy offers lower premiums but carries the risk of under-insurance. If the actual damage exceeds the insured amount, the excess loss must be borne by the policyholder.',
      basePremium: 6000,
      maxCoverageAmount: 3000000,
      durationMonths: 12,
      risksCovered: ['Fire', 'Lightning', 'Explosion'],
      propertyTypesEligible: ['Small Residential Buildings', 'Individual Flats', 'Small Shops', 'Storage Units'],
      termsAndConditions: 'Lower premium but coverage limited to specified amount. Under-insurance may result in proportionate claim settlement. Premium rate: 0.15% - 0.25% of insured value.',
      icon: 'house',
      color: 'green'
    },
    {
      name: 'Comprehensive Fire Insurance Policy',
      type: 'COMMERCIAL',
      description: 'Wider protection including theft, natural disasters, floods, and earthquakes',
      coverageDetails: 'All-inclusive coverage for multiple perils beyond standard fire insurance',
      detailedDescription: 'Comprehensive Fire Insurance Policy offers the widest protection among fire insurance policies. In addition to standard fire risks, it covers theft, burglary, natural disasters, floods, earthquakes, explosions, and other perils. This policy is ideal for homes and businesses requiring maximum protection with a single comprehensive plan.',
      basePremium: 15000,
      maxCoverageAmount: 8000000,
      durationMonths: 12,
      risksCovered: ['Fire', 'Lightning', 'Explosion', 'Theft/Burglary', 'Natural Disasters', 'Floods', 'Earthquake', 'Storm Damage', 'Malicious Damage', 'Impact Damage'],
      propertyTypesEligible: ['Residential Buildings', 'Commercial Complexes', 'Industrial Units', 'Retail Stores', 'Hotels', 'Hospitals'],
      termsAndConditions: 'Premium rate: 0.25% - 0.6% of property value per year. No-claim bonus available. 24/7 emergency assistance. Claims processed within 30 days of documentation.',
      icon: 'security',
      color: 'indigo'
    },
    {
      name: 'Consequential Loss Policy (Loss of Profit)',
      type: 'INDUSTRIAL',
      description: 'Covers business profit loss and fixed expenses due to fire interruption',
      coverageDetails: 'Business interruption insurance for loss of profit and ongoing expenses',
      detailedDescription: 'Consequential Loss Policy, also known as Business Interruption Insurance, covers financial losses when a fire forces business operations to halt. It compensates for loss of profit, fixed expenses (rent, salaries, loan EMIs), and additional expenses incurred to resume operations. This policy is essential for factories, manufacturing units, and large commercial enterprises.',
      basePremium: 30000,
      maxCoverageAmount: 15000000,
      durationMonths: 12,
      risksCovered: ['Loss of Net Profit', 'Fixed Operating Expenses', 'Salary and Wages', 'Rent and Utilities', 'Loan Interest', 'Additional Working Expenses'],
      propertyTypesEligible: ['Factories', 'Manufacturing Units', 'Production Facilities', 'Large Commercial Enterprises', 'Industrial Plants'],
      termsAndConditions: 'Indemnity period: 3-12 months. Premium based on gross profit and turnover. Requires audited financial statements. Coverage includes increased cost of working during restoration period.',
      icon: 'factory',
      color: 'orange'
    },
    {
      name: 'Replacement Value Policy',
      type: 'RESIDENTIAL',
      description: 'Pays full rebuilding/replacement cost without depreciation',
      coverageDetails: 'Full replacement cost coverage without depreciation deductions',
      detailedDescription: 'Replacement Value Policy pays the current cost to rebuild or replace damaged property without deducting depreciation. Unlike standard policies that account for age and wear, this policy ensures you receive the full amount needed to restore your property to its original condition with new materials and current construction costs.',
      basePremium: 18000,
      maxCoverageAmount: 12000000,
      durationMonths: 12,
      risksCovered: ['Fire', 'Lightning', 'Explosion', 'Natural Disasters', 'Accidental Damage'],
      propertyTypesEligible: ['Residential Buildings', 'Villas', 'Premium Apartments', 'Commercial Buildings', 'Heritage Structures'],
      termsAndConditions: 'Premium rate: 0.3% - 0.7% of replacement value. No depreciation applied on claims. Requires periodic property valuation updates. Rebuilding at current market rates guaranteed.',
      icon: 'construction',
      color: 'teal'
    }
  ];

  constructor(private fb: FormBuilder) {
    this.policyForm = this.fb.group({
      policyName: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100), CustomValidators.noWhitespace()]],
      policyType: ['', [Validators.required]],
      coverageDetails: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(500), CustomValidators.noWhitespace()]],
      detailedDescription: ['', [Validators.maxLength(2000)]],
      basePremium: [0, [Validators.required, Validators.min(1000), Validators.max(1000000), CustomValidators.positiveNumber()]],
      maxCoverageAmount: [0, [Validators.required, Validators.min(100000), Validators.max(100000000), CustomValidators.positiveNumber()]],
      durationMonths: [12, [Validators.required, Validators.min(1), Validators.max(120), CustomValidators.positiveNumber()]],
      risksCovered: [''],
      propertyTypesEligible: [''],
      termsAndConditions: ['', [Validators.maxLength(2000)]]
    });
  }

  ngOnInit(): void {
    this.loadPolicies();
  }

  loadPolicies(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.adminService.getAllPolicies().subscribe({
      next: (data) => {
        this.policies.set(data);
        this.filteredPolicies.set(data);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading policies:', error);
        this.errorMessage.set('Failed to load policies');
        this.isLoading.set(false);
      }
    });
  }

  searchPolicies(term: string): void {
    this.searchTerm.set(term);
    const lowerTerm = term.toLowerCase();
    
    if (!lowerTerm) {
      this.filteredPolicies.set(this.policies());
      return;
    }

    const filtered = this.policies().filter(policy =>
      (policy.policyName?.toLowerCase().includes(lowerTerm)) ||
      (policy.coverageDetails?.toLowerCase().includes(lowerTerm)) ||
      (policy.policyId?.toString().includes(lowerTerm))
    );

    this.filteredPolicies.set(filtered);
  }

  deletePolicy(policyId: number): void {
    if (!confirm('Are you sure you want to delete this policy?')) {
      return;
    }

    this.adminService.deletePolicy(policyId).subscribe({
      next: () => {
        const updated = this.policies().filter(p => p.policyId !== policyId);
        this.policies.set(updated);
        this.filteredPolicies.set(updated);
      },
      error: (error) => {
        console.error('Error deleting policy:', error);
        alert('Failed to delete policy');
      }
    });
  }

  openAddModal(): void {
    this.showAddModal.set(true);
    this.policyForm.reset({
      policyName: '',
      policyType: '',
      coverageDetails: '',
      detailedDescription: '',
      basePremium: 0,
      maxCoverageAmount: 0,
      durationMonths: 12,
      risksCovered: '',
      propertyTypesEligible: '',
      termsAndConditions: ''
    });
  }

  closeAddModal(): void {
    this.showAddModal.set(false);
    this.policyForm.reset();
  }

  openTemplatesModal(): void {
    this.showTemplatesModal.set(true);
  }

  closeTemplatesModal(): void {
    this.showTemplatesModal.set(false);
  }

  selectTemplate(template: PolicyTemplate): void {
    this.showTemplatesModal.set(false);
    this.policyForm.patchValue({
      policyName: template.name,
      policyType: template.type,
      coverageDetails: template.coverageDetails,
      detailedDescription: template.detailedDescription,
      basePremium: template.basePremium,
      maxCoverageAmount: template.maxCoverageAmount,
      durationMonths: template.durationMonths,
      risksCovered: template.risksCovered.join(', '),
      propertyTypesEligible: template.propertyTypesEligible.join(', '),
      termsAndConditions: template.termsAndConditions
    });
    this.showAddModal.set(true);
  }

  getTemplateIconColor(color: string): string {
    const colorMap: Record<string, string> = {
      'blue': 'text-blue-600 bg-blue-50',
      'purple': 'text-purple-600 bg-purple-50',
      'green': 'text-green-600 bg-green-50',
      'indigo': 'text-indigo-600 bg-indigo-50',
      'orange': 'text-orange-600 bg-orange-50',
      'teal': 'text-teal-600 bg-teal-50'
    };
    return colorMap[color] || 'text-gray-600 bg-gray-50';
  }

  onSubmit(): void {
    if (this.policyForm.invalid) {
      this.policyForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    const formData = {
      ...this.policyForm.value,
      risksCovered: this.policyForm.value.risksCovered ? 
        this.policyForm.value.risksCovered.split(',').map((r: string) => r.trim()).filter((r: string) => r) : [],
      propertyTypesEligible: this.policyForm.value.propertyTypesEligible ? 
        this.policyForm.value.propertyTypesEligible.split(',').map((p: string) => p.trim()).filter((p: string) => p) : []
    };

    this.adminService.createPolicy(formData).subscribe({
      next: (newPolicy) => {
        const updated = [...this.policies(), newPolicy];
        this.policies.set(updated);
        this.searchPolicies(this.searchTerm());
        this.isLoading.set(false);
        this.closeAddModal();
      },
      error: (error) => {
        console.error('Error creating policy:', error);
        this.errorMessage.set('Failed to create policy');
        this.isLoading.set(false);
      }
    });
  }

  getErrorMessage(controlName: string): string {
    const field = this.policyForm.get(controlName);
    if (field && field.invalid && (field.dirty || field.touched)) {
      return ValidationMessages.getErrorMessage(controlName, field.errors);
    }
    return '';
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.policyForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  isFieldValid(fieldName: string): boolean {
    const field = this.policyForm.get(fieldName);
    return !!(field && field.valid && field.dirty);
  }
}
