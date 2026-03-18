import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { 
  Customer, 
  UpdateCustomerProfileRequest,
  CreatePropertyRequest,
  SubscriptionRequest,
  CreateClaimRequest,
  Subscription,
  PremiumBreakdown,
  CreateEndorsementRequest,
  Endorsement
} from '../../../core/models/customer.model';
import { Property } from '../../../core/models/property.model';
import { Claim } from '../../../core/models/claim.model';
import { Policy } from '../../../core/models/policy.model';
import { Document, DocumentUploadRequest } from '../../../core/models/document.model';

@Injectable({
  providedIn: 'root'
})
export class CustomerService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api';

  // Profile Management
  getMyProfile(): Observable<Customer> {
    return this.http.get<Customer>(`${this.apiUrl}/customers/me`);
  }

  updateMyProfile(data: UpdateCustomerProfileRequest): Observable<Customer> {
    return this.http.put<Customer>(`${this.apiUrl}/customers/me`, data);
  }

  // Property Management
  getMyProperties(): Observable<Property[]> {
    return this.http.get<Property[]>(`${this.apiUrl}/properties/me`);
  }

  addProperty(data: CreatePropertyRequest): Observable<Property> {
    return this.http.post<Property>(`${this.apiUrl}/properties`, data);
  }

  updateProperty(id: number, data: CreatePropertyRequest): Observable<Property> {
    return this.http.put<Property>(`${this.apiUrl}/properties/${id}`, data);
  }

  deleteProperty(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/properties/${id}`);
  }

  // Subscription Management
  getMySubscriptions(): Observable<Subscription[]> {
    return this.http.get<Subscription[]>(`${this.apiUrl}/subscriptions/me`);
  }

  subscribeToPolicy(data: SubscriptionRequest): Observable<Subscription> {
    return this.http.post<Subscription>(`${this.apiUrl}/subscriptions`, data);
  }

  getAllPolicies(): Observable<Policy[]> {
    return this.http.get<Policy[]>(`${this.apiUrl}/policies`);
  }

  getMyPolicies(): Observable<Policy[]> {
    return this.http.get<Policy[]>(`${this.apiUrl}/policies/me`);
  }

  // Claims Management
  getMyClaims(): Observable<Claim[]> {
    return this.http.get<Claim[]>(`${this.apiUrl}/claims/me`);
  }

  fileClaim(data: CreateClaimRequest): Observable<Claim> {
    return this.http.post<Claim>(`${this.apiUrl}/claims`, data);
  }

  // Document Management
  getMyDocuments(): Observable<Document[]> {
    return this.http.get<Document[]>(`${this.apiUrl}/documents/me`);
  }

  uploadDocument(data: DocumentUploadRequest): Observable<Document> {
    const formData = new FormData();
    formData.append('file', data.file);
    formData.append('linkedEntityType', data.linkedEntityType);
    formData.append('linkedEntityId', data.linkedEntityId.toString());
    formData.append('documentType', data.documentType);

    return this.http.post<Document>(`${this.apiUrl}/documents`, formData);
  }

  // Policy Renewal & NCB Management
  getRenewalEligibleSubscriptions(): Observable<Subscription[]> {
    return this.http.get<Subscription[]>(`${this.apiUrl}/subscriptions/me/renewal-eligible`);
  }

  renewPolicy(subscriptionId: number): Observable<Subscription> {
    return this.http.post<Subscription>(`${this.apiUrl}/subscriptions/${subscriptionId}/renew`, {});
  }

  payForSubscription(subscriptionId: number): Observable<Subscription> {
    return this.http.post<Subscription>(`${this.apiUrl}/subscriptions/${subscriptionId}/pay`, {});
  }

  getPremiumBreakdown(subscriptionId: number): Observable<PremiumBreakdown> {
    return this.http.get<PremiumBreakdown>(`${this.apiUrl}/subscriptions/${subscriptionId}/premium-breakdown`);
  }

  createEndorsement(data: CreateEndorsementRequest): Observable<Endorsement> {
    return this.http.post<Endorsement>(`${this.apiUrl}/endorsements`, data);
  }

  getSubscriptionEndorsements(subscriptionId: number): Observable<Endorsement[]> {
    return this.http.get<Endorsement[]>(`${this.apiUrl}/endorsements/subscription/${subscriptionId}`);
  }

  downloadCoverNote(subscriptionId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/subscriptions/${subscriptionId}/documents/cover-note`, {
      responseType: 'blob'
    });
  }

  downloadPolicyDocument(subscriptionId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/subscriptions/${subscriptionId}/documents/policy`, {
      responseType: 'blob'
    });
  }

  getNCBBenefits(subscriptionId: number): Observable<{ benefits: string; discount: number }> {
    return this.http.get<{ benefits: string; discount: number }>(`${this.apiUrl}/subscriptions/${subscriptionId}/ncb-benefits`);
  }

  calculateNCBDiscount(years: number): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/subscriptions/ncb-calculator/${years}`);
  }
}
