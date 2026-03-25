import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, Underwriter } from '../../services/admin.service';
import { PolicySubscription } from '../../../../core/models/policy.model';
import { DocumentService } from '../../../../core/services/document.service';
import { Document } from '../../../../core/models/document.model';

@Component({
  selector: 'app-admin-subscriptions',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './subscriptions.html'
})
export class AdminSubscriptionsComponent implements OnInit {
  private adminService = inject(AdminService);

  subscriptions = signal<PolicySubscription[]>([]);
  filteredSubscriptions = signal<PolicySubscription[]>([]);

  underwriters = signal<Underwriter[]>([]);
  selectedUnderwriters = signal<Record<number, number>>({});
  
  isLoading = signal(true);
  errorMessage = signal('');
  successMessage = signal('');
  activeFilter = signal('ALL');
  
  showDocsModal = signal(false);
  currentDocs = signal<Document[]>([]);
  docsLoading = signal(false);
  selectedSubscriptionForDocs = signal<PolicySubscription | null>(null);

  private documentService = inject(DocumentService);

  ngOnInit(): void {
    this.loadUnderwriters();
    this.loadSubscriptions();
  }

  loadUnderwriters(): void {
    this.adminService.getAllUnderwriters().subscribe({
      next: (data) => this.underwriters.set(data || []),
      error: (err) => console.error('Error loading underwriters:', err)
    });
  }

  loadSubscriptions(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.adminService.getAllSubscriptions().subscribe({
      next: (data) => {
        const subs = data || [];
        
        // Deduplicate subscriptions: only keep the most recent one for each property-policy combination
        const uniqueSubs = new Map<string, PolicySubscription>();
        
        // Sort ascending by subscriptionId so the last one in the map is the latest
        const sortedSubs = [...subs].sort((a, b) => {
          const idA = a.subscriptionId || a.id || 0;
          const idB = b.subscriptionId || b.id || 0;
          return idA - idB;
        });
        
        sortedSubs.forEach(s => {
          const key = `${s.propertyId}-${s.policyName || s.requestedPolicyType || s.policyId}`;
          uniqueSubs.set(key, s);
        });
        
        this.subscriptions.set(Array.from(uniqueSubs.values()));
        this.applyFilter();
        this.isLoading.set(false);
      },
      error: (err: Error) => {
        console.error('Error loading subscriptions:', err);
        this.errorMessage.set('Failed to load subscriptions');
        this.isLoading.set(false);
      }
    });
  }

  setFilter(filter: string): void {
    this.activeFilter.set(filter);
    this.applyFilter();
  }

  applyFilter(): void {
    const filter = this.activeFilter();
    if (filter === 'ALL') {
      this.filteredSubscriptions.set(this.subscriptions());
    } else {
      this.filteredSubscriptions.set(this.subscriptions().filter(s => s.status === filter));
    }
  }

  viewDocuments(sub: PolicySubscription): void {
    const propertyId = sub.propertyId;
    this.selectedSubscriptionForDocs.set(sub);
    this.showDocsModal.set(true);
    this.docsLoading.set(true);
    this.currentDocs.set([]);
    
    this.documentService.getDocumentsForEntity(propertyId, 'POLICY_SUBSCRIPTION').subscribe({
      next: (docs) => {
        // Filter out docs that might not be proposal forms just in case
        const policyDocs = (docs || []).filter(d => 
          (d.documentType as string) === 'PROPOSAL_FORM' || (d as any).documentStage === 'POLICY_STAGE'
        );
        this.currentDocs.set(policyDocs);
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
    this.selectedSubscriptionForDocs.set(null);
  }

  downloadDocument(doc: Document): void {
    this.documentService.downloadDocument(doc.documentId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = window.document.createElement('a');
        a.href = url;
        a.download = doc.fileName || `document-${doc.documentId}`;
        window.document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        a.remove();
      },
      error: (err) => {
        console.error('Error downloading document', err);
        this.errorMessage.set('Failed to download document');
        setTimeout(() => this.errorMessage.set(''), 4000);
      }
    });
  }

  onUnderwriterSelect(subId: number, event: Event): void {
    const select = event.target as HTMLSelectElement;
    const current = this.selectedUnderwriters();
    this.selectedUnderwriters.set({ ...current, [subId]: Number(select.value) });
  }

  assignUnderwriter(sub: PolicySubscription): void {
    const subId = sub.subscriptionId || sub.id || 0;
    const underwriterId = this.selectedUnderwriters()[subId];
    if (!underwriterId) {
      this.errorMessage.set('Please select an underwriter first');
      setTimeout(() => this.errorMessage.set(''), 3000);
      return;
    }
    
    this.adminService.assignUnderwriterToSubscription(subId, underwriterId).subscribe({
      next: (response) => {
        this.successMessage.set('Underwriter assigned successfully');
        
        // Update local state to hide the assign button immediately
        const updatedSubs = this.subscriptions().map(s => {
          if ((s.subscriptionId || s.id) === subId) {
            return { ...s, underwriterId: underwriterId };
          }
          return s;
        });
        this.subscriptions.set(updatedSubs);
        this.applyFilter();

        setTimeout(() => this.successMessage.set(''), 3000);
      },
      error: (err: any) => {
        console.error('Error assigning underwriter:', err);
        const msg = err.error?.message || err.message || 'Failed to assign underwriter';
        this.errorMessage.set(msg);
        setTimeout(() => this.errorMessage.set(''), 4000);
      }
    });
  }

  approveSubscription(id: number): void {
    this.adminService.approveSubscription(id).subscribe({
      next: () => {
        this.successMessage.set('Subscription approved successfully!');
        this.loadSubscriptions();
        setTimeout(() => this.successMessage.set(''), 3000);
      },
      error: (err: any) => {
        console.error('Error approving subscription:', err);
        const msg = err.error?.message || err.message || 'Failed to approve subscription';
        this.errorMessage.set(msg);
        setTimeout(() => this.errorMessage.set(''), 5000);
      }
    });
  }

  rejectSubscription(id: number): void {
    if (!confirm('Are you sure you want to reject this subscription?')) return;
    this.adminService.rejectSubscription(id).subscribe({
      next: () => {
        this.successMessage.set('Subscription rejected.');
        this.loadSubscriptions();
        setTimeout(() => this.successMessage.set(''), 3000);
      },
      error: (err: Error) => {
        console.error('Error rejecting subscription:', err);
        this.errorMessage.set('Failed to reject subscription');
      }
    });
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      'REQUESTED': 'bg-fire-red/10 text-fire-red-700',
      'PENDING': 'bg-yellow-100 text-yellow-800',
      'UNDER_INSPECTION': 'bg-purple-100 text-purple-800',
      'INSPECTING': 'bg-purple-100 text-purple-800',
      'INSPECTED': 'bg-teal-100 text-teal-800',
      'APPROVED': 'bg-green-100 text-green-800',
      'UNDER_REVIEW': 'bg-indigo-100 text-indigo-800',
      'INSPECTION_PENDING': 'bg-orange-100 text-orange-800',
      'ACTIVE': 'bg-green-100 text-green-800',
      'REJECTED': 'bg-red-100 text-red-800',
      'CANCELLED': 'bg-gray-100 text-gray-800',
      'EXPIRED': 'bg-orange-100 text-orange-800'
    };
    return map[status] || 'bg-gray-100 text-gray-800';
  }

  getCount(status: string): number {
    if (status === 'ALL') return this.subscriptions().length;
    return this.subscriptions().filter(s => s.status === status).length;
  }

  getRiskLabel(riskScore: number): string {
    if (riskScore <= 2) return 'Low';
    if (riskScore <= 4) return 'Normal';
    if (riskScore <= 6) return 'Moderate';
    if (riskScore <= 8) return 'High';
    return 'Very High';
  }

  getRiskClass(riskScore: number): string {
    if (riskScore <= 2) return 'bg-green-100 text-green-800';
    if (riskScore <= 4) return 'bg-fire-red/10 text-fire-red-700';
    if (riskScore <= 6) return 'bg-yellow-100 text-yellow-800';
    if (riskScore <= 8) return 'bg-orange-100 text-orange-800';
    return 'bg-red-100 text-red-800';
  }
}
