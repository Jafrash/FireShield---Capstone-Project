export interface Property {
  propertyId: number;
  customerId: number;
  address: string;
  propertyType: string;
  constructionType: string;
  areaSqft: number;
  riskScore: number;
  latitude?: number;
  longitude?: number;
  zipCode?: string;
  inspectionStatus?: InspectionStatus;
  inspectionDate?: string;
  surveyorId?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface PropertyInspection {
  id: number;
  propertyId: number;
  surveyorId: number;
  inspectionDate: string;
  status: InspectionStatus;
  riskAssessment: string;
  recommendations: string;
  approvalStatus: ApprovalStatus;
  createdAt: string;
  updatedAt: string;
}

export type InspectionStatus = 'PENDING' | 'SCHEDULED' | 'COMPLETED' | 'APPROVED' | 'REJECTED';
export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
