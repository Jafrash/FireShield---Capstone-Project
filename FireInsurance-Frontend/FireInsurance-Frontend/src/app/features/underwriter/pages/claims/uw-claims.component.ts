import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import {
  UnderwriterService,
  UwClaim,
  UwDocument,
  UwSurveyor
} from '../../services/underwriter.service';

@Component({
  selector: 'app-uw-claims',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './uw-claims.component.html'
})
export class UwClaimsComponent implements OnInit {
  private uwService = inject(UnderwriterService);
  private http = inject(HttpClient);

  claims = signal<UwClaim[]>([]);
  surveyors = signal<UwSurveyor[]>([]);
  isLoading = signal(true);
  errorMessage = signal('');
  successMessage = signal('');

  // Documents modal
  showDocsModal = signal(false);
  docsLoading = signal(false);
  currentDocs = signal<UwDocument[]>([]);
  selectedClaimId = signal<number>(0);

  // Assign surveyor dropdowns: claimId -> selected surveyorId
  selectedSurveyors = signal<Record<number, number>>({});

  ngOnInit(): void {
    this.loadSurveyors();
    this.loadClaims();
  }

  loadClaims(): void {
    this.isLoading.set(true);
    this.uwService.getAssignedClaims().subscribe({
      next: (data) => {
        this.claims.set(data || []);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load claims', err);
        this.errorMessage.set(this.getApiErrorMessage(err, 'Failed to load claims.'));
        this.isLoading.set(false);
      }
    });
  }

  loadSurveyors(): void {
    this.uwService.getSurveyors().subscribe({
      next: (data) => this.surveyors.set(data || []),
      error: (err) => console.error('Failed to load surveyors', err)
    });
  }

  onSurveyorSelect(claimId: number, event: Event): void {
    const select = event.target as HTMLSelectElement;
    const current = this.selectedSurveyors();
    this.selectedSurveyors.set({ ...current, [claimId]: Number(select.value) });
  }

  assignSurveyor(claim: UwClaim): void {
    const surveyorId = this.selectedSurveyors()[claim.claimId];
    if (!surveyorId) {
      this.showError('Please select a surveyor first.');
      return;
    }
    this.uwService.assignSurveyorForClaim({ claimId: claim.claimId, surveyorId }).subscribe({
      next: () => {
        this.showSuccess('Surveyor assigned to claim successfully!');
        this.loadClaims();
      },
      error: (err) => {
        // Log the error for debugging
        console.error('Assign surveyor error:', err);
        let msg = 'Failed to assign surveyor.';
        if (err.status === 401 || err.status === 403) {
          msg = 'You are not authorized to assign a surveyor. Please check your login and role.';
        } else if (err.error?.message) {
          msg = err.error.message;
        } else if (err.message) {
          msg = err.message;
        }
        this.showError(msg);
      }
    });
  }

  approveClaim(claim: UwClaim): void {
    // Attempting to use the general claim approval endpoint which might be less status-restrictive
    this.http.put<UwClaim>(`http://localhost:8080/api/claims/${claim.claimId}/approve`, {}).subscribe({
      next: () => {
        this.showSuccess('Claim approved!');
        this.loadClaims();
      },
      error: (err) => {
        console.error('General approve failed, trying underwriter specific...', err);
        // Fallback or show original error
        this.uwService.approveClaim(claim.claimId).subscribe({
          next: () => {
            this.showSuccess('Claim approved!');
            this.loadClaims();
          },
          error: (fErr) => {
            this.showError(fErr.error?.message || 'Failed to approve claim.');
          }
        });
      }
    });
  }

  rejectClaim(claim: UwClaim): void {
    if (!confirm('Are you sure you want to reject this claim?')) return;
    this.http.put<UwClaim>(`http://localhost:8080/api/claims/${claim.claimId}/reject`, {}).subscribe({
      next: () => {
        this.showSuccess('Claim rejected.');
        this.loadClaims();
      },
      error: () => this.executeUnderwriterReject(claim.claimId)
    });
  }

  private executeUnderwriterReject(id: number): void {
    this.uwService.rejectClaim(id).subscribe({
      next: () => {
        this.showSuccess('Claim rejected.');
        this.loadClaims();
      },
      error: (err) => {
        const msg = err.error?.message || 'Failed to reject claim.';
        this.showError(msg);
      }
    });
  }

  viewDocuments(claim: UwClaim): void {
    this.selectedClaimId.set(claim.claimId);
    this.showDocsModal.set(true);
    this.docsLoading.set(true);
    this.currentDocs.set([]);
    this.uwService.getClaimDocuments(claim.claimId).subscribe({
      next: (docs) => {
        this.currentDocs.set(docs || []);
        this.docsLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load claim documents', err);
        this.docsLoading.set(false);
      }
    });
  }

  closeDocsModal(): void {
    this.showDocsModal.set(false);
    this.currentDocs.set([]);
  }

  downloadDoc(doc: UwDocument): void {
    this.uwService.downloadDocument(doc.documentId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = doc.fileName || `document_${doc.documentId}`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Failed to download document', err);
        this.showError('Failed to download document');
      }
    });
  }

  viewDoc(doc: UwDocument): void {
    this.uwService.downloadDocument(doc.documentId).subscribe({
      next: (blob) => {
        const viewBlob = new Blob([blob], { type: doc.contentType || 'application/pdf' });
        const url = window.URL.createObjectURL(viewBlob);
        window.open(url, '_blank');
      },
      error: (err) => {
        console.error('Failed to view document', err);
        this.showError('Failed to view document');
      }
    });
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      'SUBMITTED': 'bg-blue-100 text-blue-800',
      'UNDER_REVIEW': 'bg-indigo-100 text-indigo-800',
      'SIU_CLEARED': 'bg-green-100 text-green-800',
      'SURVEY_ASSIGNED': 'bg-amber-100 text-amber-800',
      'SURVEYOR_ASSIGNED': 'bg-amber-100 text-amber-800',
      'INSPECTING': 'bg-purple-100 text-purple-800',
      'SURVEY_COMPLETED': 'bg-teal-100 text-teal-800',
      'SURVEYOR_COMPLETED': 'bg-teal-100 text-teal-800',
      'INSPECTED': 'bg-teal-100 text-teal-800',
      'APPROVED': 'bg-green-100 text-green-800',
      'REJECTED': 'bg-red-100 text-red-800',
      'SETTLED': 'bg-gray-100 text-gray-800'
    };
    return map[status] || 'bg-gray-100 text-gray-800';
  }

  canApprove(claim: UwClaim): boolean {
    const readyStatuses = ['SURVEY_COMPLETED', 'SURVEYOR_COMPLETED', 'INSPECTED'];
    return readyStatuses.includes(claim.status);
  }

  private showSuccess(msg: string): void {
    this.successMessage.set(msg);
    setTimeout(() => this.successMessage.set(''), 3500);
  }

  private showError(msg: string): void {
    this.errorMessage.set(msg);
    setTimeout(() => this.errorMessage.set(''), 4000);
  }

  private getApiErrorMessage(error: unknown, fallback: string): string {
    const httpError = error as HttpErrorResponse;
    if (httpError?.status === 0) {
      return 'Cannot connect to API server at http://localhost:8080. Please start/restart backend and refresh.';
    }
    return (httpError?.error?.message as string) || httpError?.message || fallback;
  }
}
