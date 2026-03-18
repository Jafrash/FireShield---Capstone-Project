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
  pendingInspections: number;
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

export interface UnderwriterRegistrationRequest {
  username: string;
  email: string;
  password: string;
  phone?: string;
  department?: string;
  region?: string;
  experienceYears?: number;
}

export interface Inspection {
  id: number;
  propertyId: number;
  surveyorId?: number;
  inspectionType: string;
  status: string;
  scheduledDate?: string;
  completedDate?: string;
  notes?: string;
  property?: Property;
  surveyor?: Surveyor;
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

  assignClaimToSurveyor(claimId: number, surveyorId: number): Observable<Claim> {
    return this.http.post<Claim>(`${this.apiUrl}/claim-inspections/assign/${claimId}?surveyorId=${surveyorId}`, {});
  }

  getAllClaimInspections(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/claim-inspections`);
  }

  // Inspection Management
  getAllInspections(): Observable<Inspection[]> {
    return this.http.get<Inspection[]>(`${this.apiUrl}/inspections`);
  }

  getInspectionById(id: number): Observable<Inspection> {
    return this.http.get<Inspection>(`${this.apiUrl}/inspections/${id}`);
  }

  assignInspectionToSurveyor(propertyId: number, surveyorId: number): Observable<Inspection> {
    return this.http.post<Inspection>(`${this.apiUrl}/inspections/assign/${propertyId}?surveyorId=${surveyorId}`, {});
  }

  updateInspectionStatus(id: number, status: string): Observable<Inspection> {
    return this.http.patch<Inspection>(`${this.apiUrl}/inspections/${id}/status`, { status });
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
}
