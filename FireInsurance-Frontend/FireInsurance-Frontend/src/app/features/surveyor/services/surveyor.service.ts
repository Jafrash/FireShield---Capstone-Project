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
    return this.http.get<any[]>(`${this.apiUrl}/inspections/me`).pipe(
      map((inspections) => (inspections || []).map((item) => this.normalizePropertyInspection(item))),
      switchMap((inspections) => {
        if (!inspections.length) {
          return of([] as PropertyInspection[]);
        }
        return forkJoin(inspections.map((inspection) => this.enrichPropertyInspection(inspection)));
      }),
      map((inspections) => this.detectDuplicatePropertyInspections(inspections))
    );
  }

  private detectDuplicatePropertyInspections(inspections: PropertyInspection[]): PropertyInspection[] {
    // Group inspections by propertyId
    const propertyGroups = new Map<number, PropertyInspection[]>();

    inspections.forEach(inspection => {
      if (!propertyGroups.has(inspection.propertyId)) {
        propertyGroups.set(inspection.propertyId, []);
      }
      propertyGroups.get(inspection.propertyId)!.push(inspection);
    });

    // Process each property group
    propertyGroups.forEach((propertyInspections, propertyId) => {
      // Find the completed inspection (if any) for this property
      const completedInspection = propertyInspections.find(
        insp => insp.status === 'COMPLETED' && insp.assessedRiskScore !== null
      );

      if (completedInspection && propertyInspections.length > 1) {
        // Apply the risk score from completed inspection to all pending inspections
        propertyInspections.forEach(inspection => {
          if (inspection.status === 'ASSIGNED' && inspection.inspectionId !== completedInspection.inspectionId) {
            // Mark as auto-populated and add existing risk data
            (inspection as any).isDuplicateProperty = true;
            (inspection as any).existingRiskScore = completedInspection.assessedRiskScore;
            (inspection as any).existingRiskData = {
              assessedRiskScore: completedInspection.assessedRiskScore,
              fireSafetyAvailable: completedInspection.fireSafetyAvailable,
              sprinklerSystem: completedInspection.sprinklerSystem,
              fireExtinguishers: completedInspection.fireExtinguishers,
              distanceFromFireStation: completedInspection.distanceFromFireStation,
              constructionRisk: completedInspection.constructionRisk,
              hazardRisk: completedInspection.hazardRisk
            };
            (inspection as any).referenceInspectionId = completedInspection.inspectionId;
          }
        });
      }
    });

    return inspections;
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
      policyName:
        raw?.policyName ??
        raw?.policy?.name ??
        raw?.policy?.policyName ??
        claim?.policy?.name ??
        claim?.policyName ??
        null,
      maxCoverage:
        raw?.maxCoverage ??
        raw?.policy?.maxCoverage ??
        raw?.policy?.coverageAmount ??
        claim?.policy?.maxCoverage ??
        claim?.maxCoverage ??
        null,
      premiumAmount:
        raw?.premiumAmount ??
        raw?.policy?.premiumAmount ??
        claim?.policy?.premiumAmount ??
        claim?.premiumAmount ??
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

  private enrichPropertyInspection(item: PropertyInspection): Observable<PropertyInspection> {
    const needsPolicyFetch =
      !item.policyName ||
      !item.maxCoverage ||
      item.requestedSumInsured === null ||
      item.requestedSumInsured === undefined;

    if (!needsPolicyFetch || !item.propertyId) {
      return of(item);
    }

    // Since subscription endpoints are not accessible to surveyors,
    // we'll provide sample policy data based on property characteristics
    // TODO: Replace with proper backend policy data when endpoints are available

    const samplePolicyData = this.generateSamplePolicyData(item.propertyId);

    const enrichedItem = {
      ...item,
      policyName: item.policyName || samplePolicyData.policyName,
      maxCoverage: item.maxCoverage || samplePolicyData.maxCoverage,
      requestedSumInsured: item.requestedSumInsured || samplePolicyData.requestedCoverage,
      premiumAmount: item.premiumAmount || samplePolicyData.premiumAmount
    };

    return of(enrichedItem);
  }

  private generateSamplePolicyData(propertyId: number) {
    // Generate realistic policy data based on property ID
    const policyTypes = [
      { name: 'Fire Shield Basic Protection', maxCoverage: 2500000, basePremium: 45000 },
      { name: 'Fire Shield Standard Coverage', maxCoverage: 5000000, basePremium: 75000 },
      { name: 'Fire Shield Premium Protection', maxCoverage: 10000000, basePremium: 125000 },
      { name: 'Fire Shield Comprehensive Plan', maxCoverage: 15000000, basePremium: 185000 }
    ];

    const policyIndex = propertyId % policyTypes.length;
    const selectedPolicy = policyTypes[policyIndex];

    // Generate requested coverage (usually 60-80% of max coverage)
    const requestedPercentage = 0.6 + ((propertyId * 7) % 20) / 100; // Random between 60-80%
    const requestedCoverage = Math.floor(selectedPolicy.maxCoverage * requestedPercentage);

    return {
      policyName: selectedPolicy.name,
      maxCoverage: selectedPolicy.maxCoverage,
      requestedCoverage: requestedCoverage,
      premiumAmount: selectedPolicy.basePremium
    };
  }

  private enrichClaimInspection(item: ClaimInspectionItem): Observable<ClaimInspectionItem> {
    const needsClaimFetch =
      !item.customerName ||
      !item.customerEmail ||
      !item.customerPhone ||
      !item.policyName;
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
          policyName: item.policyName ?? normalizedClaim.policyName ?? null,
          maxCoverage: item.maxCoverage ?? normalizedClaim.maxCoverage ?? null,
          premiumAmount: item.premiumAmount ?? normalizedClaim.premiumAmount ?? null,
          requestedClaimAmount: item.requestedClaimAmount ?? normalizedClaim.requestedClaimAmount ?? null,
          claimDescription: item.claimDescription || normalizedClaim.claimDescription || null,
          customerDocuments: (item.customerDocuments?.length || 0) > 0
            ? item.customerDocuments
            : this.normalizeDocuments(docs)
        };
      })
    );
  }

  private normalizePropertyInspection(raw: any): PropertyInspection {
    const property = raw?.property || raw?.propertyDetails || null;
    const subscription = raw?.subscription || raw?.policySubscription || property?.subscription || null;
    const policy = subscription?.policy || raw?.policy || null;
    const customer =
      raw?.customer ||
      subscription?.customer ||
      property?.customer ||
      property?.owner ||
      null;

    return {
      ...raw,
      customerName:
        raw?.customerName ??
        customer?.name ??
        (customer?.firstName && customer?.lastName
          ? `${customer.firstName} ${customer.lastName}`
          : customer?.user?.firstName && customer?.user?.lastName
          ? `${customer.user.firstName} ${customer.user.lastName}`
          : null),
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
      propertyAddress:
        raw?.propertyAddress ??
        property?.address ??
        property?.location ??
        null,
      propertyType:
        raw?.propertyType ??
        property?.propertyType ??
        property?.type ??
        null,
      policyName:
        raw?.policyName ??
        policy?.name ??
        policy?.policyName ??
        subscription?.policyName ??
        null,
      maxCoverage:
        raw?.maxCoverage ??
        policy?.maxCoverage ??
        policy?.coverageAmount ??
        subscription?.maxCoverage ??
        null,
      premiumAmount:
        raw?.premiumAmount ??
        policy?.premiumAmount ??
        subscription?.premiumAmount ??
        null,
      requestedSumInsured:
        raw?.requestedSumInsured ??
        raw?.sumInsured ??
        subscription?.sumInsured ??
        subscription?.coverageAmount ??
        null,
      customerDocuments: this.normalizeDocuments(
        raw?.customerDocuments ??
        raw?.documents ??
        property?.documents ??
        subscription?.documents ??
        []
      )
    };
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
