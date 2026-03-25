export interface Claim {
  claimId: number;
  subscriptionId: number;
  claimNumber: string;
  incidentDate: string;
  claimDate: string;
  claimAmount: number;
  description: string;
  status: ClaimStatus;
  inspectionStatus: ClaimInspectionStatus;
  surveyorId?: number;
  underwriterId?: number;
  settlementAmount?: number;
  estimatedLoss?: number;
  deductible?: number;
  depreciation?: number;
  createdAt: string;
  updatedAt: string;

  // Fraud Detection Fields
  fraudScore?: number;
  riskLevel?: RiskLevel;
  fraudStatus?: FraudStatus;
  siuInvestigatorId?: number;
  investigationNotes?: string;
  fraudAnalysisTimestamp?: string;
}

export interface ClaimInspection {
  id: number;
  claimId: number;
  surveyorId: number;
  inspectionDate: string;
  status: ClaimInspectionStatus;
  damageAssessment: string;
  estimatedLoss: number;
  recommendations: string;
  createdAt: string;
  updatedAt: string;
}

export type ClaimStatus = 'SUBMITTED' | 'UNDER_REVIEW' | 'INSPECTING' | 'INSPECTED' | 'APPROVED' | 'REJECTED' | 'SETTLED' | 'PAID' | 'SURVEY_ASSIGNED' | 'SURVEY_COMPLETED';
export type ClaimInspectionStatus = 'PENDING' | 'SCHEDULED' | 'COMPLETED' | 'APPROVED' | 'REJECTED' | 'ASSIGNED' | 'IN_PROGRESS' | 'UNDER_REVIEW';

// Fraud Detection Types
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type FraudStatus = 'CLEAR' | 'FLAGGED' | 'UNDER_REVIEW' | 'SIU_INVESTIGATION' | 'CLEARED' | 'CONFIRMED_FRAUD';
export type BlacklistType = 'USER' | 'PHONE' | 'ADDRESS' | 'EMAIL';

// Fraud Analysis Response
export interface FraudAnalysis {
  claimId: number;
  fraudScore: number;
  riskLevel: RiskLevel;
  fraudStatus: FraudStatus;
  ruleBreakdown: FraudRuleResult[];
  analysisTimestamp: string;
  recommendation: string;
}

export interface FraudRuleResult {
  ruleName: string;
  ruleDescription: string;
  scoreContribution: number;
  triggered: boolean;
  details: string;
}

// Blacklist Interfaces
export interface BlacklistEntry {
  id: number;
  type: BlacklistType;
  value: string;
  reason: string;
  createdAt: string;
  createdBy: string;
  isActive: boolean;
}

export interface BlacklistRequest {
  type: BlacklistType;
  value: string;
  reason: string;
}

// SIU Request Interfaces
export interface SiuAssignmentRequest {
  siuInvestigatorId: number;
  initialNotes?: string;
}

export interface InvestigationNotesRequest {
  notes: string;
  newStatus?: FraudStatus;
}
