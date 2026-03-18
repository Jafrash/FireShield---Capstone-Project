import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map, of, switchMap, catchError } from 'rxjs';
import {
  PropertyInspection,
  ClaimInspectionItem,
  InspectionDocumentSummary,
  SubmitInspectionReportRequest,
  SubmitClaimInspectionReportRequest
} from '../../../core/models/inspection.model';

export interface SurveyorDashboardStats {
  assignedPropertyInspections: number;
  assignedClaimInspections: number;
  completedInspections: number;
  pendingInspections: number;
}

@Injectable({
  providedIn: 'root'
})
export class SurveyorService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api';

  // Dashboard Stats — derived from both inspection lists
  getDashboardStats(): Observable<SurveyorDashboardStats> {
    return forkJoin({
      propertyInspections: this.getMyPropertyInspections(),
      claimInspections: this.getMyClaimInspections()
    }).pipe(
      map(({ propertyInspections, claimInspections }) => {
        const assignedProp = propertyInspections.filter(i => i.status === 'ASSIGNED').length;
        const completedProp = propertyInspections.filter(i => i.status === 'COMPLETED').length;
        const assignedClaim = claimInspections.filter(i => i.status === 'ASSIGNED' || i.status === 'UNDER_REVIEW').length;
        const completedClaim = claimInspections.filter(i => i.status === 'APPROVED' || i.status === 'REJECTED').length;
        return {
          assignedPropertyInspections: assignedProp,
          assignedClaimInspections: assignedClaim,
          completedInspections: completedProp + completedClaim,
          pendingInspections: assignedProp + assignedClaim
        };
      })
    );
  }

  // Property Inspections
  getMyPropertyInspections(): Observable<PropertyInspection[]> {
    return this.http.get<PropertyInspection[]>(`${this.apiUrl}/inspections/me`);
  }

  submitPropertyInspectionReport(id: number, data: SubmitInspectionReportRequest): Observable<PropertyInspection> {
    return this.http.put<PropertyInspection>(`${this.apiUrl}/inspections/${id}/submit`, data);
  }

  getPropertyInspectionById(id: number): Observable<PropertyInspection> {
    return this.http.get<PropertyInspection>(`${this.apiUrl}/inspections/${id}`);
  }

  // Claim Inspections
  getMyClaimInspections(): Observable<ClaimInspectionItem[]> {
    return this.http.get<any[]>(`${this.apiUrl}/claim-inspections/me`).pipe(
      map((inspections) => (inspections || []).map((item) => this.normalizeClaimInspection(item))),
      switchMap((inspections) => {
        if (!inspections.length) {
          return of([] as ClaimInspectionItem[]);
        }
        return forkJoin(inspections.map((inspection) => this.enrichClaimInspection(inspection)));
      })
    );
  }

  submitClaimInspectionReport(id: number, data: SubmitClaimInspectionReportRequest): Observable<ClaimInspectionItem> {
    return this.http.put<ClaimInspectionItem>(`${this.apiUrl}/claim-inspections/${id}/submit`, data);
  }

  getClaimInspectionById(id: number): Observable<ClaimInspectionItem> {
    return this.http.get<ClaimInspectionItem>(`${this.apiUrl}/claim-inspections/${id}`);
  }

  // Profile
  getMyProfile(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/surveyors/me`);
  }

  updateMyProfile(data: UpdateSurveyorRequest): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/surveyors/me`, data);
  }

  private normalizeClaimInspection(raw: any): ClaimInspectionItem {
    const claim = raw?.claim || raw?.claimDetails || null;
    const customer =
      raw?.customer ||
      claim?.customer ||
      claim?.subscription?.customer ||
      claim?.policySubscription?.customer ||
      null;

    const firstName =
      customer?.firstName ??
      customer?.first_name ??
      customer?.user?.firstName ??
      customer?.user?.first_name ??
      '';
    const lastName =
      customer?.lastName ??
      customer?.last_name ??
      customer?.user?.lastName ??
      customer?.user?.last_name ??
      '';
    const derivedCustomerName = `${firstName} ${lastName}`.trim();

    return {
      inspectionId: Number(raw?.inspectionId ?? raw?.claimInspectionId ?? raw?.id ?? 0),
      claimId: Number(raw?.claimId ?? claim?.claimId ?? claim?.id ?? 0),
      surveyorName: raw?.surveyorName ?? raw?.surveyor?.name ?? raw?.surveyor?.username ?? 'Unknown',
      inspectionDate: raw?.inspectionDate ?? raw?.createdAt ?? null,
      estimatedLoss: raw?.estimatedLoss ?? raw?.assessedLoss ?? null,
      status: (raw?.status ?? 'ASSIGNED') as ClaimInspectionItem['status'],
      customerName: (raw?.customerName ?? derivedCustomerName) || null,
      customerEmail:
        raw?.customerEmail ??
        customer?.email ??
        customer?.user?.email ??
        null,
      customerPhone:
        raw?.customerPhone ??
        customer?.phoneNumber ??
        customer?.phone ??
        customer?.user?.phoneNumber ??
        null,
      requestedClaimAmount:
        raw?.requestedClaimAmount ??
        raw?.claimAmount ??
        raw?.requestedAmount ??
        claim?.claimAmount ??
        claim?.requestedClaimAmount ??
        claim?.requestedAmount ??
        null,
      claimDescription: raw?.claimDescription ?? claim?.description ?? null,
      customerDocuments: this.normalizeDocuments(
        raw?.customerDocuments ?? raw?.documents ?? raw?.claimDocuments ?? claim?.documents ?? []
      )
    };
  }

  private enrichClaimInspection(item: ClaimInspectionItem): Observable<ClaimInspectionItem> {
    const needsClaimFetch =
      !item.customerName ||
      !item.customerEmail ||
      !item.customerPhone ||
      item.requestedClaimAmount == null;
    const needsDocumentsFetch = (item.customerDocuments?.length || 0) === 0;

    const claim$ = needsClaimFetch && item.claimId
      ? this.http.get<any>(`${this.apiUrl}/claims/${item.claimId}`).pipe(catchError(() => of(null)))
      : of(null);

    const docs$ = needsDocumentsFetch && item.claimId
      ? this.http.get<any[]>(`${this.apiUrl}/documents/claim/${item.claimId}`).pipe(catchError(() => of([])))
      : of(item.customerDocuments || []);

    return forkJoin({ claim: claim$, docs: docs$ }).pipe(
      map(({ claim, docs }) => {
        const normalizedClaim = claim ? this.normalizeClaimInspection({
          ...item,
          claim,
          claimId: item.claimId,
          inspectionId: item.inspectionId
        }) : item;

        return {
          ...item,
          customerName: item.customerName || normalizedClaim.customerName || null,
          customerEmail: item.customerEmail || normalizedClaim.customerEmail || null,
          customerPhone: item.customerPhone || normalizedClaim.customerPhone || null,
          requestedClaimAmount:
            item.requestedClaimAmount ?? normalizedClaim.requestedClaimAmount ?? null,
          claimDescription: item.claimDescription || normalizedClaim.claimDescription || null,
          customerDocuments: (item.customerDocuments?.length || 0) > 0
            ? item.customerDocuments
            : this.normalizeDocuments(docs)
        };
      })
    );
  }

  private normalizeDocuments(source: any[]): InspectionDocumentSummary[] {
    return (source || []).map((doc: any) => ({
      documentId: Number(doc?.documentId ?? doc?.id ?? 0),
      fileName: doc?.fileName ?? doc?.name ?? 'document',
      documentType: doc?.documentType ?? doc?.type ?? 'OTHER',
      documentStage: doc?.documentStage ?? doc?.stage ?? 'CLAIM_STAGE',
      uploadDate: doc?.uploadDate ?? doc?.createdAt ?? null,
      uploadedBy: doc?.uploadedBy ?? doc?.uploadedByUsername ?? doc?.owner ?? 'customer'
    }));
  }
}

export interface UpdateSurveyorRequest {
  phoneNumber?: string;
  licenseNumber?: string;
  experienceYears?: number;
  assignedRegion?: string;
}
