import { SubscriptionStatus } from './policy.model';

export interface Customer {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  phone?: string;
  address: string;
  city: string;
  state: string;
  createdAt: string;
  updatedAt: string;
  // Fallbacks for various backend naming conventions (Java, Spring, snake_case, etc.)
  first_name?: string;
  last_name?: string;
  created_at?: string;
  updated_at?: string;
  memberSince?: string;
  joiningDate?: string;
  registeredDate?: string;
  createdDate?: string;
  updatedDate?: string;
  lastLogin?: string;
  last_login?: string;
}

export interface AdminCustomerView extends Customer {
  propertiesCount?: number;
  policiesCount?: number;
  active?: boolean;
}

export interface UpdateCustomerProfileRequest {
  firstName?: string;
  lastName?: string;
  phoneNumber?: string; // Changed from phone
  address?: string;
  city?: string;
  state?: string;
}

export interface CreatePropertyRequest {
  propertyType: string;
  address: string;
  areaSqft: number;
  constructionType: string;
}

export interface SubscriptionRequest {
  propertyId: number;
  policyId: number;
  requestedCoverage?: number; // Optional coverage amount requested by customer
  constructionType?: string;
  roofType?: string;
  numberOfFloors?: number;
  occupancyType?: string;
  manufacturingProcess?: string;
  hazardousGoods?: string;
  previousLossHistory?: string;
  insuranceDeclinedBefore?: boolean;
  propertyValue?: number;
}

export interface CreateClaimRequest {
  subscriptionId: number;
  description: string;
  claimAmount: number;
  incidentDate: string;
}

export interface CreateEndorsementRequest {
  subscriptionId: number;
  changeType: string;
  requestedCoverage?: number;
  newOccupancyType?: string;
  newHazardousGoods?: string;
  reason: string;
}

export interface Endorsement {
  endorsementId: number;
  subscriptionId: number;
  changeType: string;
  requestedCoverage?: number;
  newOccupancyType?: string;
  newHazardousGoods?: string;
  reason: string;
  status: 'REQUESTED' | 'APPROVED' | 'REJECTED';
  requestedBy: string;
  reviewedBy?: string;
  createdAt: string;
  reviewedAt?: string;
}

export interface PremiumBreakdownLineItem {
  label: string;
  description: string;
  amount: number;
  kind: 'BASE' | 'ADJUSTMENT' | 'DISCOUNT';
}

export interface PremiumBreakdown {
  subscriptionId: number;
  policyName: string;
  totalPremium: number;
  monthlyPremium: number;
  installmentMonths: number;
  approvedCoverage: number;
  requestedCoverage: number;
  maxCoverageAmount: number;
  basePremiumReference: number;
  derivedBaseRate: number;
  basePremiumForCoverage: number;
  riskScore: number;
  baseRiskMultiplier: number;
  rawRiskFactor: number;
  finalRiskFactor: number;
  deductible: number;
  lineItems: PremiumBreakdownLineItem[];
}

export interface Subscription {
  subscriptionId: number;
  policyId?: number; // Optional since backend may or may not include it
  propertyId: number;
  policyName: string;
  startDate: string;
  endDate: string;
  status: SubscriptionStatus;
  premiumAmount: number;
  inspectionId?: number; // Link to property inspection

  // NEW: Risk-based premium calculation fields
  basePremiumAmount?: number;  // Policy base premium before risk adjustment
  riskScore?: number;          // Surveyor's risk assessment (0-10)
  riskMultiplier?: number;     // Applied multiplier (0.8 - 2.0)

  // NEW: Policy Renewal & NCB Fields
  renewalEligible?: boolean;        // True if policy is within 30 days of expiry
  previousSubscriptionId?: number;  // Links to previous subscription in renewal chain
  renewalCount?: number;            // Number of times this policy has been renewed
  daysRemaining?: number;           // Days until policy expires
  claimFreeYears?: number;          // Consecutive claim-free years
  ncbDiscount?: number;             // No Claim Bonus discount (0.0 - 0.5)

  // NEW: Payment & legal document fields
  paymentReceived?: boolean;
  coverNoteFileName?: string;
  policyDocumentFileName?: string;

  // NEW: Proposal fields
  constructionType?: string;
  roofType?: string;
  numberOfFloors?: number;
  occupancyType?: string;
  manufacturingProcess?: string;
  hazardousGoods?: string;
  previousLossHistory?: string;
  insuranceDeclinedBefore?: boolean;
  propertyValue?: number;
}

export interface PolicyTemplate {
  id: number;
  policyType: string;
  description: string;
  basePremium: number;
  coverageRange: {
    min: number;
    max: number;
  };
}
