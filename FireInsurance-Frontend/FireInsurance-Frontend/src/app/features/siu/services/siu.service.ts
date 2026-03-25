import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

// SIU-specific interfaces for real data
export interface SiuClaim {
  claimId: string;
  fraudScore: number;
  state: string;
  policyNumber?: string;
  customerName?: string;
  claimAmount?: number;
  priority?: 'HIGH' | 'MEDIUM' | 'LOW';
  assignedDate?: string;
  lastActivity?: string;
  suspiciousIndicators?: string[];
}

// Detailed claim interface for SIU investigation
export interface SiuClaimDetails {
  claimId: string;
  subscriptionId?: number;
  description: string;
  claimAmount: number;
  status: string;
  createdAt: string;
  estimatedLoss: number;
  calculatedDeductible: number;
  calculatedDepreciation: number;
  settlementAmount: number;
  underwriterId?: number;
  fraudScore: number;
  incidentDate: string;
  causeOfFire?: string;
  firNumber?: string;
  fireBrigadeReportNumber?: string;
  salvageDetails?: string;

  // Customer information
  customer?: {
    customerId: number;
    firstName: string;
    lastName: string;
    email: string;
    phoneNumber: string;
    address: string;
    city: string;
    state: string;
  };

  // Policy information
  policy?: {
    policyId: number;
    policyName: string;
    policyType: string;
    maxCoverageAmount: number;
    deductible: number;
  };

  // Property information
  property?: {
    propertyId: number;
    propertyType: string;
    address: string;
    propertyValue: number;
    propertyAge?: number;
  };
}

export interface SiuClaimsResponse {
  claims: SiuClaim[];
  totalCount: number;
  highPriorityCount: number;
  suspiciousCount: number;
  averageFraudScore: number;
  totalAmount: number;
}

// Investigation action interfaces
export interface SiuInvestigationActionRequest {
  claimId: string;
  investigatorId?: string;
  notes?: string;
  reason?: string;
}

export interface SiuInvestigationActionResponse {
  success: boolean;
  message: string;
  claimId: string;
  newStatus: string;
  timestamp: string;
}

@Injectable({
  providedIn: 'root'
})
export class SiuService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/siu`;

  /**
   * Get all SIU claims assigned for investigation
   */
  getClaims(): Observable<SiuClaimsResponse> {
    return this.http.get<SiuClaimsResponse>(`${this.apiUrl}/claims`);
  }

  /**
   * Get specific claim details for investigation from SIU endpoint
   * Note: Using SIU-specific endpoint for proper role-based access control
   */
  getClaimDetails(claimId: string): Observable<SiuClaimDetails> {
    return this.http.get<SiuClaimDetails>(`${this.apiUrl}/claims/${claimId}`);
  }

  /**
   * Update claim investigation status
   */
  updateClaimStatus(claimId: string, status: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/claims/${claimId}/status`, { status });
  }

  /**
   * Submit investigation report
   */
  submitInvestigationReport(claimId: string, report: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/claims/${claimId}/report`, report);
  }

  // === Investigation Action Methods ===

  /**
   * Start formal SIU investigation on a claim
   */
  startInvestigation(claimId: string, notes?: string): Observable<SiuInvestigationActionResponse> {
    const request: SiuInvestigationActionRequest = {
      claimId,
      notes: notes || 'Investigation started by SIU'
    };
    return this.http.post<SiuInvestigationActionResponse>(`${this.apiUrl}/start-investigation`, request);
  }

  /**
   * Mark claim as fraudulent after investigation
   */
  markAsFraud(claimId: string, reason?: string): Observable<SiuInvestigationActionResponse> {
    const request: SiuInvestigationActionRequest = {
      claimId,
      reason: reason || 'Fraudulent activity confirmed by SIU investigation'
    };
    return this.http.post<SiuInvestigationActionResponse>(`${this.apiUrl}/mark-fraud`, request);
  }

  /**
   * Clear claim as legitimate after investigation
   */
  clearClaim(claimId: string, notes?: string): Observable<SiuInvestigationActionResponse> {
    const request: SiuInvestigationActionRequest = {
      claimId,
      notes: notes || 'Claim cleared as legitimate by SIU investigation'
    };
    return this.http.post<SiuInvestigationActionResponse>(`${this.apiUrl}/clear-claim`, request);
  }
}