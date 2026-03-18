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
  customerDocuments?: InspectionDocumentSummary[];
  fireSafetyAvailable?: boolean;
  sprinklerSystem?: boolean;
  fireExtinguishers?: boolean;
  distanceFromFireStation?: number | null;
  constructionRisk?: number | null;
  hazardRisk?: number | null;
  recommendedCoverage?: number | null;
  recommendedPremium?: number | null;
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
  requestedClaimAmount?: number | null;
  claimDescription?: string | null;
  customerDocuments?: InspectionDocumentSummary[];
}

export interface SubmitClaimInspectionReportRequest {
  estimatedLoss: number;
  damageReport: string;
}
