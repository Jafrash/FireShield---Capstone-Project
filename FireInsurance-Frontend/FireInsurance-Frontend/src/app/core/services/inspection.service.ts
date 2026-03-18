import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { Surveyor } from '../../features/admin/services/admin.service';

export interface AssignInspectionRequest {
  subscriptionId: number;
  propertyId: number;
  surveyorId: number;
}

export interface InspectionDetails {
  inspectionId: number;
  subscriptionId: number;
  propertyId: number;
  surveyorId: number;
  surveyorName?: string;
  status: string;
  assignedDate?: string;
}

@Injectable({
  providedIn: 'root'
})
export class InspectionService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api';

  getSurveyors(): Observable<Surveyor[]> {
    return this.http.get<Surveyor[]>(`${this.apiUrl}/surveyors`);
  }

  assignInspection(data: AssignInspectionRequest): Observable<InspectionDetails> {
    // Correctly routes to the Subscription controller which creates the Inspection internally
    return this.http.put<any>(`${this.apiUrl}/subscriptions/${data.subscriptionId}/assign-surveyor/${data.surveyorId}`, {}).pipe(
      map(res => ({
        inspectionId: 0, // Not immediately needed by front-end row state
        subscriptionId: data.subscriptionId,
        propertyId: data.propertyId,
        surveyorId: data.surveyorId,
        status: res.status, // Should return PENDING
        assignedDate: new Date().toISOString()
      }))
    );
  }

  getInspectionStatus(subscriptionId: number): Observable<InspectionDetails> {
    return this.http.get<InspectionDetails>(`${this.apiUrl}/inspections/subscription/${subscriptionId}`);
  }
}
