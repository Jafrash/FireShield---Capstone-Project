// Matches backend InspectionResponse DTO
export interface InspectionDocumentSummary {
  documentId: number;
  fileName: string;
  documentType: string;
  documentStage: string;
  uploadDate: string | null;
  uploadedBy: string;
}

export interface PropertyInspection {
  inspectionId: number;
  propertyId: number;
  surveyorName: string;
  inspectionDate: string | null;
  assessedRiskScore: number | null;
  status: 'ASSIGNED' | 'COMPLETED' | 'REJECTED';
  customerName?: string | null;
  customerEmail?: string | null;
  customerPhone?: string | null;
  propertyAddress?: string | null;
  propertyType?: string | null;
  policyName?: string | null; // Name of the policy customer is subscribing to
  maxCoverage?: number | null; // Maximum coverage limit of the policy
  premiumAmount?: number | null; // Premium amount for the policy
  requestedSumInsured?: number | null; // Customer's requested coverage amount
  customerDocuments?: InspectionDocumentSummary[];
  fireSafetyAvailable?: boolean;
  sprinklerSystem?: boolean;
  fireExtinguishers?: boolean;
  distanceFromFireStation?: number | null;
  constructionRisk?: number | null;
  hazardRisk?: number | null;
  recommendedCoverage?: number | null;
  recommendedPremium?: number | null;
  constructionType?: string | null;
  roofType?: string | null;
  occupancyType?: string | null;
  electricalAuditStatus?: string | null;
  hazardousMaterialsPresent?: boolean;
  adjacentBuildingDistance?: number | null;
  internalProtectionNotes?: string | null;
  // Duplicate property detection fields
  isDuplicateProperty?: boolean; // True if this property has already been inspected
  existingRiskScore?: number | null; // Risk score from the completed inspection
  existingRiskData?: {
    assessedRiskScore: number;
    fireSafetyAvailable?: boolean;
    sprinklerSystem?: boolean;
    fireExtinguishers?: boolean;
    distanceFromFireStation?: number | null;
    constructionRisk?: number | null;
    hazardRisk?: number | null;
  };
  referenceInspectionId?: number; // ID of the completed inspection this data comes from
}

export interface SubmitInspectionReportRequest {
  assessedRiskScore: number;
  remarks: string;
  fireSafetyAvailable?: boolean;
  sprinklerSystem?: boolean;
  fireExtinguishers?: boolean;
  distanceFromFireStation?: number;
  constructionRisk?: number;
  hazardRisk?: number;
  recommendedCoverage?: number;
  recommendedPremium?: number;
  constructionType?: string;
  roofType?: string;
  occupancyType?: string;
  electricalAuditStatus?: string;
  hazardousMaterialsPresent?: boolean;
  adjacentBuildingDistance?: number;
  internalProtectionNotes?: string;
}

// Matches backend ClaimInspectionResponse DTO
export interface ClaimInspectionItem {
  inspectionId: number;
  claimId: number;
  surveyorName: string;
  inspectionDate: string | null;
  estimatedLoss: number | null;
  status: 'ASSIGNED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED';
  customerName?: string | null;
  customerEmail?: string | null;
  customerPhone?: string | null;
  policyName?: string | null;
  maxCoverage?: number | null;
  premiumAmount?: number | null;
  requestedClaimAmount?: number | null;
  claimDescription?: string | null;
  customerDocuments?: InspectionDocumentSummary[];
  // Professional fields
  causeOfFire?: string | null;
  salvageValue?: number | null;
  fireBrigadeExpenses?: number | null;
  otherInsuranceDetails?: string | null;
  underInsuranceDetected?: boolean | null;
  recommendedSettlement?: number | null;
}

export interface SubmitClaimInspectionReportRequest {
  estimatedLoss: number;
  damageReport: string;
  causeOfFire?: string;
  salvageValue?: number;
  fireBrigadeExpenses?: number;
  otherInsuranceDetails?: string;
  underInsuranceDetected?: boolean;
  recommendedSettlement?: number;
}
