import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface PublicPolicy {
  policyId: number;
  policyName: string;
  coverageDetails: string;
  basePremium: number;
  maxCoverageAmount: number;
  durationMonths: number;
}

export interface QuoteRequest {
  propertyType: string;
  buildingArea: number;
  constructionType: string;
  city: string;
}

@Injectable({
  providedIn: 'root'
})
export class PublicService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  getPolicies(): Observable<PublicPolicy[]> {
    return this.http.get<PublicPolicy[]>(`${this.apiUrl}/policies`);
  }

  requestQuote(data: QuoteRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/quotes`, data);
  }
}
