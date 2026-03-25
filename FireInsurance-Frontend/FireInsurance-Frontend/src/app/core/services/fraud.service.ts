import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  FraudAnalysis,
  BlacklistEntry,
  BlacklistRequest,
  BlacklistType,
  Claim
} from '../models/claim.model';
import { API_BASE_URL } from '../constants';

/**
 * Interface for SIU Case Response (to avoid JSON circular references)
 */
export interface SiuCaseResponse {
  claimId: number;
  claimNumber?: string;
  description?: string;
  status: string;
  fraudStatus: string;
  claimAmount?: number;
  fraudScore?: number;
  riskLevel: string;
  siuInvestigatorId?: number;
  investigatorName?: string;
  createdAt?: string;
  customerName?: string;
}

/**
 * Interface for SIU Investigator
 */
export interface SiuInvestigator {
  investigatorId: number;
  username: string;
  email: string;
  phoneNumber: string;
  badgeNumber: string;
  specialization: string;
  experienceYears: number;
  active: boolean;
  createdAt: string;
}

/**
 * Interface for Investigation Case
 */
export interface InvestigationCase {
  investigationId: number;
  claim: any; // Will be populated with Claim object
  assignedInvestigator: SiuInvestigator;
  status: InvestigationStatus;
  assignedDate: string;
  startedDate?: string;
  completedDate?: string;
  initialNotes?: string;
  priorityLevel: number;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
}

/**
 * Investigation Status enum type
 */
export type InvestigationStatus = 'ASSIGNED' | 'INVESTIGATING' | 'UNDER_REVIEW' | 'COMPLETED';

/**
 * Request interface for creating investigation case
 */
export interface CreateInvestigationCaseRequest {
  claimId: number;
  investigatorId: number;
  initialNotes?: string;
}

/**
 * Request interface for updating case status
 */
export interface UpdateCaseStatusRequest {
  status: InvestigationStatus;
}

/**
 * Request interface for adding investigation note
 */
export interface AddInvestigationNoteRequest {
  note: string;
}

/**
 * Interface for case statistics
 */
export interface CaseStatistics {
  totalCases: number;
  assignedCases: number;
  investigatingCases: number;
  underReviewCases: number;
  completedCases: number;
  highPriorityCases: number;
}

/**
 * Service for fraud detection and blacklist management.
 */
@Injectable({
  providedIn: 'root'
})
export class FraudService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${API_BASE_URL}/fraud`;

  // ==================== BLACKLIST MANAGEMENT ====================

  /**
   * Get all active blacklist entries.
   */
  getAllBlacklist(): Observable<BlacklistEntry[]> {
    return this.http.get<BlacklistEntry[]>(`${this.apiUrl}/blacklist`);
  }

  /**
   * Get blacklist entries by type.
   */
  getBlacklistByType(type: BlacklistType): Observable<BlacklistEntry[]> {
    return this.http.get<BlacklistEntry[]>(`${this.apiUrl}/blacklist/type/${type}`);
  }

  /**
   * Add a new entry to the blacklist.
   */
  addToBlacklist(request: BlacklistRequest): Observable<BlacklistEntry> {
    return this.http.post<BlacklistEntry>(`${this.apiUrl}/blacklist`, request);
  }

  /**
   * Remove (soft-delete) a blacklist entry.
   */
  removeFromBlacklist(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/blacklist/${id}`);
  }

  // ==================== FRAUD ANALYSIS ====================

  /**
   * Get fraud analysis for a specific claim.
   */
  getFraudAnalysis(claimId: number): Observable<FraudAnalysis> {
    return this.http.get<FraudAnalysis>(`${this.apiUrl}/analysis/${claimId}`);
  }

  /**
   * Recalculate fraud score for an existing claim.
   */
  recalculateFraudScore(claimId: number): Observable<FraudAnalysis> {
    return this.http.post<FraudAnalysis>(`${this.apiUrl}/analysis/${claimId}/recalculate`, {});
  }

  // ==================== PATTERN DETECTION ====================

  /**
   * Check if a customer has frequent claims.
   */
  checkFrequentClaims(customerId: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.apiUrl}/patterns/frequent-claims/${customerId}`);
  }

  /**
   * Check if an address is used by multiple customers.
   */
  checkDuplicateAddress(address: string, excludeCustomerId: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.apiUrl}/patterns/duplicate-address`, {
      params: { address, excludeCustomerId: excludeCustomerId.toString() }
    });
  }

  // ==================== SIU CASE MANAGEMENT ====================

  /**
   * Get all available SIU investigators.
   */
  getSiuInvestigators(): Observable<SiuInvestigator[]> {
    return this.http.get<SiuInvestigator[]>(`${this.apiUrl}/siu/investigators`);
  }

  /**
   * Get all claims under SIU investigation.
   */
  getSiuCases(): Observable<SiuCaseResponse[]> {
    return this.http.get<SiuCaseResponse[]>(`${this.apiUrl}/siu/cases`);
  }

  /**
   * Get claims assigned to a specific SIU investigator.
   */
  getClaimsByInvestigator(investigatorId: number): Observable<SiuCaseResponse[]> {
    return this.http.get<SiuCaseResponse[]>(`${this.apiUrl}/siu/investigator/${investigatorId}/cases`);
  }

  /**
   * Get case statistics for a specific investigator.
   */
  getInvestigatorStatistics(investigatorId: number): Observable<CaseStatistics> {
    return this.http.get<CaseStatistics>(`${this.apiUrl}/siu/investigator/${investigatorId}/statistics`);
  }

  // ==================== ENHANCED SIU INVESTIGATION CASE MANAGEMENT ====================

  /**
   * Create a new investigation case for a claim.
   */
  createInvestigationCase(request: CreateInvestigationCaseRequest): Observable<InvestigationCase> {
    return this.http.post<InvestigationCase>(`${this.apiUrl}/siu/cases/create`, request);
  }

  /**
   * Get all investigation cases assigned to a specific investigator.
   */
  getAssignedCases(investigatorId: number): Observable<InvestigationCase[]> {
    return this.http.get<InvestigationCase[]>(`${this.apiUrl}/siu/investigator/${investigatorId}/assigned-cases`);
  }

  /**
   * Get complete details of a specific investigation case.
   */
  getCaseDetails(investigationId: number): Observable<InvestigationCase> {
    return this.http.get<InvestigationCase>(`${this.apiUrl}/siu/case/${investigationId}/details`);
  }

  /**
   * Update the status of an investigation case.
   */
  updateCaseStatus(investigationId: number, request: UpdateCaseStatusRequest): Observable<InvestigationCase> {
    return this.http.post<InvestigationCase>(`${this.apiUrl}/siu/case/${investigationId}/update-status`, request);
  }

  /**
   * Add investigation note to a case.
   */
  addInvestigationNote(investigationId: number, request: AddInvestigationNoteRequest): Observable<InvestigationCase> {
    return this.http.post<InvestigationCase>(`${this.apiUrl}/siu/case/${investigationId}/add-note`, request);
  }

  /**
   * Get all investigation cases for SIU dashboard overview.
   */
  getAllInvestigationCases(): Observable<InvestigationCase[]> {
    return this.http.get<InvestigationCase[]>(`${this.apiUrl}/siu/cases/all`);
  }

  /**
   * Debug endpoint to check investigator assignments
   */
  debugInvestigatorData(investigatorId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/siu/debug/investigator/${investigatorId}`);
  }

  /**
   * Sync claim assignments with investigation cases (admin repair function)
   */
  syncClaimAssignments(): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/siu/sync-assignments`, {});
  }

}
