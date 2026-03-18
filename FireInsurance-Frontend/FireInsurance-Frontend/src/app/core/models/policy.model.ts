export interface Policy {
  policyId: number;
  policyName: string;
  policyType?: string;
  coverageDetails: string;
  detailedDescription?: string;
  basePremium: number; // Base annual premium
  maxCoverageAmount: number;
  durationMonths: number;
  risksCovered?: string[];
  propertyTypesEligible?: string[];
  termsAndConditions?: string;
  
  // Enhanced insurance-specific fields
  deductible?: number; // Amount customer pays before insurance kicks in
  claimSettlementRatio?: number; // Percentage (e.g., 95 means 95% of claims settled)
  exclusions?: string[]; // What's not covered
  coverageBenefits?: string[]; // Detailed list of benefits
  addOns?: string[]; // Optional riders/add-ons available
  sumInsuredMin?: number; // Minimum sum insured
  cashlessGarages?: number; // For auto insurance (not applicable for fire, but good to have)
  premiumCalculationFactors?: {
    riskMultiplier: number; // Based on property risk assessment
    locationFactor: number; // Fire risk zone factor
    constructionFactor: number; // Building material factor
    ageFactor: number; // Age-based depreciation factor
  };
  
  createdAt?: string;
  updatedAt?: string;
}

export type PolicyStatus = 'ACTIVE' | 'EXPIRED' | 'CANCELLED' | 'PENDING';

export type SubscriptionStatus = 
  | 'SUBMITTED'    // Customer submitted proposal form
  | 'REQUESTED'    // Customer applied for policy
  | 'PENDING'      // Admin reviewing, surveyor assigned  
  | 'INSPECTING'   // Surveyor inspecting property
  | 'INSPECTED'    // Inspection completed, waiting admin approval
  | 'UNDER_INSPECTION'
  | 'INSPECTION_PENDING'
  | 'UNDER_REVIEW'
  | 'APPROVED'
  | 'PAYMENT_PENDING'
  | 'ACTIVE'       // Policy approved and active
  | 'EXPIRED'      // Policy term ended
  | 'CANCELLED'    // Policy cancelled
  | 'REJECTED';    // Policy application rejected

export interface PolicySubscription {
  id: number;
  subscriptionId?: number; // Alias for id
  customerId: number;
  propertyId: number;
  policyId?: number;
  policyName?: string;
  requestedCoverage: number;
  requestedPolicyType: string;
  status: SubscriptionStatus;
  premiumAmount?: number;
  annualPremium?: number;
  subscriptionDate?: string;
  startDate?: string;
  endDate?: string;
  createdAt: string;
  updatedAt?: string;
  
  // NEW: Risk-based premium calculation fields
  basePremiumAmount?: number;  // Policy base premium before risk adjustment
  riskScore?: number;          // Surveyor's risk assessment (0-10)
  riskMultiplier?: number;     // Applied multiplier (0.8 - 2.0)
  inspectionId?: number;       // Link to property inspection
  underwriterId?: number;      // Assigned underwriter
}

// Request DTOs for creating/updating policies
export interface CreatePolicyRequest {
  policyName: string;
  policyType?: string;
  coverageDetails: string;
  detailedDescription?: string;
  basePremium: number;
  maxCoverageAmount: number;
  durationMonths: number;
  risksCovered?: string[];
  propertyTypesEligible?: string[];
  termsAndConditions?: string;
}

export interface UpdatePolicyRequest {
  policyName?: string;
  policyType?: string;
  coverageDetails?: string;
  detailedDescription?: string;
  basePremium?: number;
  maxCoverageAmount?: number;
  durationMonths?: number;
  risksCovered?: string[];
  propertyTypesEligible?: string[];
  termsAndConditions?: string;
}
