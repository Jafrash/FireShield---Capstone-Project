export interface Claim {
  claimId: number;
  subscriptionId: number;
  claimNumber: string;
  incidentDate: string;
  createdAt: string;
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
  updatedAt: string;
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

export type ClaimStatus = 'SUBMITTED' | 'UNDER_REVIEW' | 'INSPECTING' | 'INSPECTED' | 'APPROVED' | 'REJECTED' | 'SETTLED' | 'PAID';
export type ClaimInspectionStatus = 'PENDING' | 'SCHEDULED' | 'COMPLETED' | 'APPROVED' | 'REJECTED';
