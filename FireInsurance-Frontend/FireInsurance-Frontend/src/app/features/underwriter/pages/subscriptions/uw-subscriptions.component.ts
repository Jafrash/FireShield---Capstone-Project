import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  UnderwriterService,
  UwSubscription,
  UwDocument,
  UwSurveyor
} from '../../services/underwriter.service';

@Component({
  selector: 'app-uw-subscriptions',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './uw-subscriptions.component.html'
})
export class UwSubscriptionsComponent implements OnInit {
  private uwService = inject(UnderwriterService);

  subscriptions = signal<UwSubscription[]>([]);
  surveyors = signal<UwSurveyor[]>([]);
  isLoading = signal(true);
  errorMessage = signal('');
  successMessage = signal('');

  // Documents modal
  showDocsModal = signal(false);
  docsLoading = signal(false);
  currentDocs = signal<UwDocument[]>([]);
  selectedSubId = signal<number>(0);

  // Assign surveyor dropdowns: subscriptionId -> selected surveyorId
  selectedSurveyors = signal<Record<number, number>>({});

  ngOnInit(): void {
    this.loadSurveyors();
    this.loadSubscriptions();
  }

  loadSubscriptions(): void {
    this.isLoading.set(true);
    this.uwService.getAssignedSubscriptions().subscribe({
      next: (data) => {
        this.subscriptions.set(data || []);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load subscriptions', err);
        this.errorMessage.set('Failed to load subscriptions.');
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

  onSurveyorSelect(subId: number, event: Event): void {
    const select = event.target as HTMLSelectElement;
    const current = this.selectedSurveyors();
    this.selectedSurveyors.set({ ...current, [subId]: Number(select.value) });
  }

  assignSurveyor(sub: UwSubscription): void {
    const surveyorId = this.selectedSurveyors()[sub.subscriptionId];
    if (!surveyorId) {
      this.showError('Please select a surveyor first.');
      return;
    }
    this.uwService.assignSurveyorForProperty({ subscriptionId: sub.subscriptionId, surveyorId }).subscribe({
      next: () => {
        this.showSuccess('Surveyor assigned successfully!');
        this.loadSubscriptions();
      },
      error: (err) => {
        const msg = err.error?.message || 'Failed to assign surveyor.';
        this.showError(msg);
      }
    });
  }

  approveSubscription(id: number): void {
    this.uwService.approveSubscription(id).subscribe({
      next: () => {
        this.showSuccess('Subscription approved!');
        this.loadSubscriptions();
      },
      error: (err) => {
        const msg = err.error?.message || 'Failed to approve subscription.';
        this.showError(msg);
      }
    });
  }

  rejectSubscription(id: number): void {
    if (!confirm('Are you sure you want to reject this subscription?')) return;
    this.uwService.rejectSubscription(id).subscribe({
      next: () => {
        this.showSuccess('Subscription rejected.');
        this.loadSubscriptions();
      },
      error: (err) => {
        const msg = err.error?.message || 'Failed to reject subscription.';
        this.showError(msg);
      }
    });
  }

  viewDocuments(sub: UwSubscription): void {
    this.selectedSubId.set(sub.subscriptionId);
    this.showDocsModal.set(true);
    this.docsLoading.set(true);
    this.currentDocs.set([]);
    this.uwService.getSubscriptionDocuments(sub.subscriptionId).subscribe({
      next: (docs) => {
        this.currentDocs.set(docs || []);
        this.docsLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load documents', err);
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
        // Create a blob with the correct type for viewing
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
      'REQUESTED': 'bg-blue-100 text-blue-800',
      'PENDING': 'bg-yellow-100 text-yellow-800',
      'UNDER_REVIEW': 'bg-blue-100 text-blue-800',
      'INSPECTION_PENDING': 'bg-amber-100 text-amber-800',
      'INSPECTING': 'bg-purple-100 text-purple-800',
      'UNDER_INSPECTION': 'bg-purple-100 text-purple-800',
      'INSPECTED': 'bg-teal-100 text-teal-800',
      'APPROVED': 'bg-green-100 text-green-800',
      'ACTIVE': 'bg-emerald-100 text-emerald-800',
      'REJECTED': 'bg-red-100 text-red-800',
      'CANCELLED': 'bg-gray-100 text-gray-800',
      'EXPIRED': 'bg-orange-100 text-orange-800'
    };
    return map[status] || 'bg-gray-100 text-gray-800';
  }

  canApprove(sub: UwSubscription): boolean {
    return sub.status === 'INSPECTED';
  }

  private showSuccess(msg: string): void {
    this.successMessage.set(msg);
    setTimeout(() => this.successMessage.set(''), 3500);
  }

  private showError(msg: string): void {
    this.errorMessage.set(msg);
    setTimeout(() => this.errorMessage.set(''), 4000);
  }
}
