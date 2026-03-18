import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UwSubscription {
  subscriptionId: number;
  propertyId?: number;
  policyName?: string;
  startDate?: string;
  endDate?: string;
  status: string;
  premiumAmount?: number;
  basePremiumAmount?: number;
  riskScore?: number;
  riskMultiplier?: number;
  inspectionId?: number;
  renewalEligible?: boolean;
  previousSubscriptionId?: number;
  renewalCount?: number;
  claimFreeYears?: number;
  ncbDiscount?: number;
  requestedCoverage?: number;
}

export interface UwClaim {
  claimId: number;
  subscriptionId?: number;
  description?: string;
  claimAmount?: number;
  status: string;
  createdAt?: string;
  estimatedLoss?: number;
  calculatedDeductible?: number;
  calculatedDepreciation?: number;
  settlementAmount?: number;
}

export interface UwDocument {
  documentId: number;
  fileName?: string;
  documentType?: string;
  documentStage?: string;
  fileSize?: number;
  contentType?: string;
  uploadDate?: string;
  uploadedBy?: string;
}

export interface UwSurveyor {
  surveyorId: number;
  username: string;
  email: string;
  phoneNumber?: string;
  licenseNumber?: string;
  experienceYears?: number;
  assignedRegion?: string;
  firstName?: string;
  lastName?: string;
}

export interface AssignSurveyorPropertyRequest {
  subscriptionId: number;
  surveyorId: number;
}

export interface AssignSurveyorClaimRequest {
  claimId: number;
  surveyorId: number;
}

@Injectable({ providedIn: 'root' })
export class UnderwriterService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api';

  getAssignedSubscriptions(): Observable<UwSubscription[]> {
    return this.http.get<UwSubscription[]>(`${this.apiUrl}/underwriter/subscriptions/assigned`);
  }

  getAssignedClaims(): Observable<UwClaim[]> {
    return this.http.get<UwClaim[]>(`${this.apiUrl}/underwriter/claims/assigned`);
  }

  getSubscriptionDocuments(id: number): Observable<UwDocument[]> {
    return this.http.get<UwDocument[]>(`${this.apiUrl}/underwriter/subscriptions/${id}/documents`);
  }

  getClaimDocuments(id: number): Observable<UwDocument[]> {
    return this.http.get<UwDocument[]>(`${this.apiUrl}/underwriter/claims/${id}/documents`);
  }

  downloadDocument(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/documents/download/${id}`, { responseType: 'blob' });
  }

  assignSurveyorForProperty(payload: AssignSurveyorPropertyRequest): Observable<UwSubscription> {
    return this.http.post<UwSubscription>(`${this.apiUrl}/underwriter/assign-surveyor/property`, payload);
  }

  assignSurveyorForClaim(payload: AssignSurveyorClaimRequest): Observable<UwClaim> {
    return this.http.post<UwClaim>(`${this.apiUrl}/underwriter/assign-surveyor/claim`, payload);
  }

  approveSubscription(id: number): Observable<UwSubscription> {
    return this.http.post<UwSubscription>(`${this.apiUrl}/underwriter/subscriptions/${id}/approve`, {});
  }

  rejectSubscription(id: number): Observable<UwSubscription> {
    return this.http.post<UwSubscription>(`${this.apiUrl}/underwriter/subscriptions/${id}/reject`, {});
  }

  approveClaim(id: number): Observable<UwClaim> {
    return this.http.post<UwClaim>(`${this.apiUrl}/underwriter/claims/${id}/approve`, {});
  }

  rejectClaim(id: number): Observable<UwClaim> {
    return this.http.post<UwClaim>(`${this.apiUrl}/underwriter/claims/${id}/reject`, {});
  }

  getSurveyors(): Observable<UwSurveyor[]> {
    return this.http.get<UwSurveyor[]>(`${this.apiUrl}/surveyors`);
  }
}
