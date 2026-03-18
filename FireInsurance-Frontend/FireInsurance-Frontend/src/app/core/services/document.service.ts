import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpEvent, HttpEventType } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Document, DocumentType, DocumentEntityType } from '../models/document.model';

export interface UploadProgress {
  progress: number;
  state: 'PENDING' | 'IN_PROGRESS' | 'DONE';
  response?: Document | any;
}

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/documents';

  uploadDocument(file: File, documentType: DocumentType, linkedEntityId: number, linkedEntityType: DocumentEntityType): Observable<UploadProgress> {
    const formData = new FormData();
    formData.append('file', file);
    
    let backendDocType = 'OTHER';
    let documentStage = 'POLICY_STAGE';
    let propertyId: number | null = null;
    let claimId: number | null = null;
    let endpoint = `${this.apiUrl}/upload/customer`;

    if (linkedEntityType === 'PROPERTY') {
      backendDocType = 'OWNERSHIP_PROOF';
      documentStage = 'POLICY_STAGE';
      propertyId = linkedEntityId;
    } else if (linkedEntityType === 'POLICY_SUBSCRIPTION') {
      backendDocType = 'PROPOSAL_FORM';
      documentStage = 'POLICY_STAGE';
      propertyId = linkedEntityId; // Link subscription docs directly to the underlying Property
    } else if (linkedEntityType === 'CLAIM') {
      backendDocType = 'CLAIM_FORM';
      documentStage = 'CLAIM_STAGE';
      claimId = linkedEntityId;
    } else if (linkedEntityType === 'INSPECTION') {
      backendDocType = 'RISK_INSPECTION_REPORT';
      documentStage = 'INSPECTION_STAGE';
      endpoint = `${this.apiUrl}/upload/surveyor`;
    } else if (linkedEntityType === 'CLAIM_INSPECTION') {
      backendDocType = 'SPOT_SURVEY_REPORT';
      documentStage = 'CLAIM_STAGE';
      endpoint = `${this.apiUrl}/upload/surveyor`;
    }

    formData.append('documentType', backendDocType);
    formData.append('documentStage', documentStage);
    if (propertyId) formData.append('propertyId', propertyId.toString());
    if (claimId) formData.append('claimId', claimId.toString());

    return this.http.post<Document>(endpoint, formData, {
      reportProgress: true,
      observe: 'events'
    }).pipe(
      map((event: HttpEvent<any>) => this.getUploadProgress(event))
    );
  }

  private getUploadProgress(event: HttpEvent<any>): UploadProgress {
    switch (event.type) {
      case HttpEventType.Sent:
        return { progress: 0, state: 'PENDING' };
      case HttpEventType.UploadProgress:
        const progress = event.total ? Math.round((100 * event.loaded) / event.total) : 0;
        return { progress, state: 'IN_PROGRESS' };
      case HttpEventType.Response:
        return { progress: 100, state: 'DONE', response: event.body };
      default:
        return { progress: 0, state: 'PENDING' };
    }
  }

  getDocumentsForEntity(entityId: number, entityType: DocumentEntityType): Observable<Document[]> {
    let endpoint = `${this.apiUrl}/property/${entityId}`; // Default/Fallback
    
    // Map to actual backend endpoints
    if (entityType === 'CLAIM' || entityType === 'CLAIM_INSPECTION') {
      endpoint = `${this.apiUrl}/claim/${entityId}`;
    } else if (entityType === 'PROPERTY' || entityType === 'POLICY_SUBSCRIPTION' || entityType === 'INSPECTION') {
      endpoint = `${this.apiUrl}/property/${entityId}`;
    }
    
    return this.http.get<Document[]>(endpoint);
  }

  deleteDocument(documentId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${documentId}`);
  }

  downloadDocument(documentId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/download/${documentId}`, { responseType: 'blob' });
  }
}

