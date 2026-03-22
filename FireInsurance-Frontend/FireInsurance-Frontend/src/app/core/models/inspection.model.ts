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
  policyName?: string | null; // Policy that covers this claim
  maxCoverage?: number | null; // Maximum coverage limit of the policy
  premiumAmount?: number | null; // Premium amount of the policy
  requestedClaimAmount?: number | null; // Amount customer requested in claim
  claimDescription?: string | null;
  customerDocuments?: InspectionDocumentSummary[];
}

export interface SubmitClaimInspectionReportRequest {
  estimatedLoss: number;
  damageReport: string;
}
