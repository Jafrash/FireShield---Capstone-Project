import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Customer } from '../../../core/models/customer.model';
import { Policy, PolicySubscription, CreatePolicyRequest, UpdatePolicyRequest } from '../../../core/models/policy.model';
import { Claim } from '../../../core/models/claim.model';
import { Property } from '../../../core/models/property.model';
import { Document } from '../../../core/models/document.model';

export interface DashboardStats {
  totalCustomers: number;
  totalClaims: number;
  activePolicies: number;
  totalSurveyors?: number;
}

export interface Surveyor {
  surveyorId: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  specialization?: string;
  status?: string;
  createdAt?: string;
  licenseNumber?: string;
  experienceYears?: number;
  assignedRegion?: string;
}

export interface Underwriter {
  underwriterId: number;
  username: string;
  email: string;
  phone?: string;
  department?: string;
  region?: string;
  experienceYears?: number;
  active?: boolean;
  createdAt?: string;
}

export interface SiuInvestigator {
  investigatorId: number;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
  badgeNumber?: string;
  department?: string;
  experienceYears?: number;
  specialization?: string;
  active?: boolean;
  createdAt?: string;
}

export interface BlacklistEntry {
  blacklistId: number;
  username: string;
  email: string;
  phoneNumber?: string;
  reason: string;
  active: boolean;
  createdAt: string;
  createdBy: string;
}

export interface BlacklistRequest {
  username: string;
  email: string;
  phoneNumber?: string;
  reason: string;
}

export interface FraudRule {
  ruleName: string;
  description: string;
  weight: number;
  triggered: boolean;
  impact: 'LOW' | 'MEDIUM' | 'HIGH';
  details: string;
}

export interface FraudAnalysisResponse {
  claimId: number;
  fraudScore: number;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  ruleBreakdown: FraudRule[];
  overallAssessment: string;
  suspiciousIndicators: string[];
  confidenceScore: number;
}

export interface FraudStatistics {
  totalFraudCases: number;
  highRiskClaims: number;
  mediumRiskClaims: number;
  lowRiskClaims: number;
  totalSiuInvestigations: number;
  activeSiuInvestigations: number;
  completedSiuInvestigations: number;
  fraudConfirmedCases: number;
  clearedCases: number;
  averageFraudScore: number;
  totalClaimsValue: number;
  fraudulentClaimsValue: number;
  fraudPercentage: number;
}

export interface FraudDistribution {
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  count: number;
  percentage: number;
  totalValue: number;
}

export interface SiuWorkload {
  investigatorId: number;
  investigatorName: string;
  activeCases: number;
  completedCases: number;
  fraudConfirmed: number;
  cleared: number;
  averageCompletionDays: number;
}

export interface FraudTrend {
  month: string;
  fraudCases: number;
  totalClaims: number;
  fraudPercentage: number;
}

export interface UnderwriterRegistrationRequest {
  username: string;
  email: string;
  password: string;
  phone?: string;
  department?: string;
  region?: string;
  experienceYears?: number;
}

export interface SiuInvestigatorRegistrationRequest {
  username: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
  badgeNumber?: string;
  department?: string;
  experienceYears?: number;
  specialization?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api';

  // Dashboard Stats
  getDashboardStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.apiUrl}/admin/dashboard/stats`);
  }

  // Customer Management
  getAllCustomers(): Observable<Customer[]> {
    return this.http.get<Customer[]>(`${this.apiUrl}/customers`);
  }

  getCustomerById(id: number): Observable<Customer> {
    return this.http.get<Customer>(`${this.apiUrl}/customers/${id}`);
  }

  deleteCustomer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/customers/${id}`);
  }

  // Surveyor Management
  getAllSurveyors(): Observable<Surveyor[]> {
    return this.http.get<Surveyor[]>(`${this.apiUrl}/surveyors`);
  }

  getSurveyorById(id: number): Observable<Surveyor> {
    return this.http.get<Surveyor>(`${this.apiUrl}/surveyors/${id}`);
  }

  createSurveyor(data: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/admin/surveyors`, data);
  }

  deleteSurveyor(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/surveyors/${id}`);
  }

  // Underwriter Management
  getAllUnderwriters(): Observable<Underwriter[]> {
    return this.http.get<Underwriter[]>(`${this.apiUrl}/admin/underwriters`);
  }

  registerUnderwriter(data: UnderwriterRegistrationRequest): Observable<Underwriter> {
    return this.http.post<Underwriter>(`${this.apiUrl}/admin/underwriters`, data);
  }

  assignUnderwriterToSubscription(subscriptionId: number, underwriterId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/admin/assign-underwriter/subscription`, { targetId: subscriptionId, underwriterId });
  }

  assignUnderwriterToClaim(claimId: number, underwriterId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/admin/assign-underwriter/claim`, { targetId: claimId, underwriterId }, {
      // Backend returns plain text message; avoid JSON parse errors treated as HttpErrorResponse.
      responseType: 'text'
    });
  }

  // SIU Investigator Management
  getAllSiuInvestigators(): Observable<SiuInvestigator[]> {
    return this.http.get<SiuInvestigator[]>(`${this.apiUrl}/admin/siu-users`);
  }

  // Alias method for getting SIU users (as per requirement)
  getSiuUsers(): Observable<SiuInvestigator[]> {
    return this.getAllSiuInvestigators();
  }

  getSiuInvestigatorById(id: number): Observable<SiuInvestigator> {
    return this.http.get<SiuInvestigator>(`${this.apiUrl}/admin/siu-investigators/${id}`);
  }

  registerSiuInvestigator(data: SiuInvestigatorRegistrationRequest): Observable<SiuInvestigator> {
    return this.http.post<SiuInvestigator>(`${this.apiUrl}/admin/siu-investigators`, data);
  }

  deleteSiuInvestigator(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/admin/siu-investigators/${id}`);
  }

  assignSiuInvestigatorToClaim(claimId: number, investigatorId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/admin/assign-siu-investigator/claim`, { targetId: claimId, investigatorId }, {
      responseType: 'text'
    });
  }

  // Assign SIU Investigator to Claim (specific endpoint)
  assignSiuToClaim(claimId: number, investigatorId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/admin/assign-siu`, { claimId, investigatorId }, {
      responseType: 'text'
    });
  }

  // Blacklist Management
  getBlacklistedUsers(): Observable<BlacklistEntry[]> {
    return this.http.get<BlacklistEntry[]>(`${this.apiUrl}/admin/blacklist`);
  }

  getBlacklistedUserById(id: number): Observable<BlacklistEntry> {
    return this.http.get<BlacklistEntry>(`${this.apiUrl}/admin/blacklist/${id}`);
  }

  addToBlacklist(data: BlacklistRequest): Observable<BlacklistEntry> {
    return this.http.post<BlacklistEntry>(`${this.apiUrl}/admin/blacklist`, data);
  }

  removeFromBlacklist(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/admin/blacklist/${id}`, {
      responseType: 'text'
    });
  }

  checkBlacklist(identifier: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.apiUrl}/admin/blacklist/check/${encodeURIComponent(identifier)}`);
  }

  // Fraud Analysis
  getFraudAnalysis(claimId: number): Observable<FraudAnalysisResponse> {
    return this.http.get<FraudAnalysisResponse>(`${this.apiUrl}/fraud/analysis/${claimId}`);
  }

  // Fraud Monitoring Dashboard
  getFraudStatistics(): Observable<FraudStatistics> {
    return this.http.get<FraudStatistics>(`${this.apiUrl}/admin/fraud/statistics`);
  }

  getFraudDistribution(): Observable<FraudDistribution[]> {
    return this.http.get<FraudDistribution[]>(`${this.apiUrl}/admin/fraud/distribution`);
  }

  getSiuWorkload(): Observable<SiuWorkload[]> {
    return this.http.get<SiuWorkload[]>(`${this.apiUrl}/admin/fraud/siu-workload`);
  }

  getFraudTrends(): Observable<FraudTrend[]> {
    return this.http.get<FraudTrend[]>(`${this.apiUrl}/admin/fraud/trends`);
  }

  // Policy Management
  getAllPolicies(): Observable<Policy[]> {
    return this.http.get<Policy[]>(`${this.apiUrl}/policies`);
  }

  getPolicyById(id: number): Observable<Policy> {
    return this.http.get<Policy>(`${this.apiUrl}/policies/${id}`);
  }

  createPolicy(data: CreatePolicyRequest): Observable<Policy> {
    return this.http.post<Policy>(`${this.apiUrl}/policies`, data);
  }

  updatePolicy(id: number, data: UpdatePolicyRequest): Observable<Policy> {
    return this.http.put<Policy>(`${this.apiUrl}/policies/${id}`, data);
  }

  deletePolicy(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/policies/${id}`);
  }

  // Claim Management
  getAllClaims(): Observable<Claim[]> {
    return this.http.get<Claim[]>(`${this.apiUrl}/claims`);
  }

  getClaimById(id: number): Observable<Claim> {
    return this.http.get<Claim>(`${this.apiUrl}/claims/${id}`);
  }

  updateClaimStatus(id: number, status: string): Observable<Claim> {
    return this.http.patch<Claim>(`${this.apiUrl}/claims/${id}/status`, { status });
  }

  approveClaim(id: number): Observable<Claim> {
    return this.http.put<Claim>(`${this.apiUrl}/claims/${id}/approve`, {});
  }

  rejectClaim(id: number): Observable<Claim> {
    return this.http.put<Claim>(`${this.apiUrl}/claims/${id}/reject`, {});
  }

  // Property Management
  getAllProperties(): Observable<Property[]> {
    return this.http.get<Property[]>(`${this.apiUrl}/properties`);
  }

  getPropertyById(id: number): Observable<Property> {
    return this.http.get<Property>(`${this.apiUrl}/properties/${id}`);
  }

  // Subscription Management
  getAllSubscriptions(): Observable<PolicySubscription[]> {
    return this.http.get<PolicySubscription[]>(`${this.apiUrl}/subscriptions`);
  }

  approveSubscription(id: number): Observable<PolicySubscription> {
    return this.http.put<PolicySubscription>(`${this.apiUrl}/subscriptions/${id}/approve`, {});
  }

  rejectSubscription(id: number): Observable<PolicySubscription> {
    return this.http.put<PolicySubscription>(`${this.apiUrl}/subscriptions/${id}/reject`, {});
  }

  cancelSubscription(id: number): Observable<PolicySubscription> {
    return this.http.put<PolicySubscription>(`${this.apiUrl}/subscriptions/${id}/cancel`, {});
  }

  // Policy by ID
  getPolicyByIdPublic(id: number): Observable<Policy> {
    return this.http.get<Policy>(`${this.apiUrl}/policies/${id}`);
  }

  // Document Management
  getAllDocuments(): Observable<Document[]> {
    return this.http.get<Document[]>(`${this.apiUrl}/documents`);
  }

  deleteDocument(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/documents/${id}`);
  }

  // Claim Inspection Report
  getClaimInspectionByClaimId(claimId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/claim-inspections/claim/${claimId}`);
  }
}
