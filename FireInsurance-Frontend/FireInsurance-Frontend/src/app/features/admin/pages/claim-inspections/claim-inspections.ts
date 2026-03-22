import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService } from '../../services/admin.service';

interface ClaimInspectionDetail {
  inspectionId: number;
  claimId: number;
  claimNumber?: string;
  surveyorName: string;
  inspectionDate: string | null;
  estimatedLoss: number | null;
  damageReport?: string;
  status: 'ASSIGNED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED';
}

@Component({
  selector: 'app-claim-inspections',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>Claim Inspections</h1>
        <p class="text-sm text-gray-600 mt-1">View and manage all claim inspections</p>
      </div>

      @if (loading()) {
        <div class="content-card">
          <div class="flex items-center justify-center py-12">
            <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-fire-red"></div>
          </div>
        </div>
      } @else if (error()) {
        <div class="content-card">
          <div class="text-center py-12">
            <p class="text-red-600">{{ error() }}</p>
            <button (click)="loadInspections()" class="mt-4 btn-primary">Retry</button>
          </div>
        </div>
      } @else if (inspections().length === 0) {
        <div class="content-card">
          <div class="text-center py-12">
            <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"></path>
            </svg>
            <h3 class="mt-4 text-lg font-medium text-gray-900">No Claim Inspections</h3>
            <p class="mt-2 text-sm text-gray-500">Claim inspections will appear here when surveyors are assigned to claims.</p>
          </div>
        </div>
      } @else {
        <div class="content-card">
          <div class="overflow-x-auto">
            <table class="min-w-full divide-y divide-gray-200">
              <thead class="bg-gray-50">
                <tr>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Inspection ID</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Claim ID</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Surveyor</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Inspection Date</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Estimated Loss</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                </tr>
              </thead>
              <tbody class="bg-white divide-y divide-gray-200">
                @for (inspection of inspections(); track inspection.inspectionId) {
                  <tr class="hover:bg-gray-50 transition-colors">
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">#{{inspection.inspectionId}}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      @if (inspection.claimNumber) {
                        {{inspection.claimNumber}}
                      } @else {
                        #{{inspection.claimId}}
                      }
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{{inspection.surveyorName}}</td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      @if (inspection.inspectionDate) {
                        {{formatDate(inspection.inspectionDate)}}
                      } @else {
                        <span class="text-gray-400">Not scheduled</span>
                      }
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      @if (inspection.estimatedLoss !== null && inspection.estimatedLoss !== undefined) {
                        <span class="font-semibold text-orange-600">
                          ₹{{formatCurrency(inspection.estimatedLoss)}}
                        </span>
                      } @else {
                        <span class="text-gray-400">Pending</span>
                      }
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap">
                      <span [class]="getStatusClass(inspection.status)">
                        {{getStatusLabel(inspection.status)}}
                      </span>
                    </td>
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                      <button 
                        (click)="viewDetails(inspection)" 
                        class="text-fire-red hover:text-fire-orange transition-colors">
                        View Details
                      </button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </div>
      }

      <!-- Details Modal -->
      @if (selectedInspection()) {
        <div class="modal-overlay" (click)="closeDetails()">
          <div class="modal-content" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <h2 class="text-xl font-bold text-gray-900">Claim Inspection Details</h2>
              <button (click)="closeDetails()" class="text-gray-400 hover:text-gray-600">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                </svg>
              </button>
            </div>
            <div class="modal-body">
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="block text-sm font-medium text-gray-500">Inspection ID</label>
                  <p class="mt-1 text-base text-gray-900">#{{selectedInspection()!.inspectionId}}</p>
                </div>
                <div>
                  <label class="block text-sm font-medium text-gray-500">Claim ID</label>
                  <p class="mt-1 text-base text-gray-900">
                    @if (selectedInspection()!.claimNumber) {
                      {{selectedInspection()!.claimNumber}}
                    } @else {
                      #{{selectedInspection()!.claimId}}
                    }
                  </p>
                </div>
                <div>
                  <label class="block text-sm font-medium text-gray-500">Surveyor</label>
                  <p class="mt-1 text-base text-gray-900">{{selectedInspection()!.surveyorName}}</p>
                </div>
                <div>
                  <label class="block text-sm font-medium text-gray-500">Status</label>
                  <p class="mt-1">
                    <span [class]="getStatusClass(selectedInspection()!.status)">
                      {{getStatusLabel(selectedInspection()!.status)}}
                    </span>
                  </p>
                </div>
                <div>
                  <label class="block text-sm font-medium text-gray-500">Inspection Date</label>
                  <p class="mt-1 text-base text-gray-900">
                    @if (selectedInspection()!.inspectionDate) {
                      {{formatDate(selectedInspection()!.inspectionDate!)}}
                    } @else {
                      <span class="text-gray-400">Not scheduled</span>
                    }
                  </p>
                </div>
                <div>
                  <label class="block text-sm font-medium text-gray-500">Estimated Loss</label>
                  <p class="mt-1">
                    @if (selectedInspection()!.estimatedLoss !== null && selectedInspection()!.estimatedLoss !== undefined) {
                      <span class="text-lg font-semibold text-orange-600">
                        ₹{{formatCurrency(selectedInspection()!.estimatedLoss!)}}
                      </span>
                    } @else {
                      <span class="text-gray-400">Pending assessment</span>
                    }
                  </p>
                </div>
                @if (selectedInspection()!.damageReport) {
                  <div class="col-span-2">
                    <label class="block text-sm font-medium text-gray-500">Damage Report</label>
                    <div class="mt-1 p-3 bg-gray-50 rounded-lg">
                      <p class="text-base text-gray-700 whitespace-pre-wrap">{{selectedInspection()!.damageReport}}</p>
                    </div>
                  </div>
                }
              </div>
            </div>
            <div class="modal-footer">
              <button (click)="closeDetails()" class="btn-secondary">Close</button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .page-container { 
      max-width: 1400px; 
      margin: 0 auto; 
      padding: 1.5rem; 
    }
    .page-header { 
      margin-bottom: 1.5rem; 
    }
    .page-header h1 { 
      font-size: 1.875rem; 
      font-weight: 700; 
      color: #111827; 
      margin: 0; 
    }
    .content-card { 
      background: white; 
      border-radius: 12px; 
      padding: 0; 
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05); 
      border: 1px solid #e5e7eb; 
      overflow: hidden;
    }

    .btn-primary {
      background: linear-gradient(to right, var(--color-fire-red), var(--color-fire-orange));
      color: white;
      padding: 0.5rem 1rem;
      border-radius: 0.5rem;
      font-weight: 500;
      transition: background-color 0.2s;
    }
    .btn-primary:hover {
      background-color: #6d1731;
    }

    .btn-secondary {
      background-color: #f3f4f6;
      color: #374151;
      padding: 0.5rem 1rem;
      border-radius: 0.5rem;
      font-weight: 500;
      transition: background-color 0.2s;
    }
    .btn-secondary:hover {
      background-color: #e5e7eb;
    }

    .modal-overlay {
      position: fixed;
      inset: 0;
      background-color: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 50;
      padding: 1rem;
    }
    .modal-content {
      background: white;
      border-radius: 12px;
      max-width: 48rem;
      width: 100%;
      max-height: 90vh;
      overflow-y: auto;
      box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
    }
    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1.5rem;
      border-bottom: 1px solid #e5e7eb;
    }
    .modal-body {
      padding: 1.5rem;
    }
    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: 0.75rem;
      padding: 1.5rem;
      border-top: 1px solid #e5e7eb;
    }

    table { width: 100%; }
    th { background-color: #f9fafb; }
  `]
})
export class ClaimInspectionsComponent implements OnInit {
  private adminService = inject(AdminService);

  inspections = signal<ClaimInspectionDetail[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  selectedInspection = signal<ClaimInspectionDetail | null>(null);

  ngOnInit() {
    this.loadInspections();
  }

  loadInspections() {
    this.loading.set(true);
    this.error.set(null);

    this.adminService.getAllClaimInspections().subscribe({
      next: (data) => {
        this.inspections.set(data as any);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Failed to load claim inspections:', err);
        this.error.set('Failed to load claim inspections. Please try again.');
        this.loading.set(false);
      }
    });
  }

  viewDetails(inspection: ClaimInspectionDetail) {
    this.selectedInspection.set(inspection);
  }

  closeDetails() {
    this.selectedInspection.set(null);
  }

  getStatusClass(status: string): string {
    const classes = 'px-3 py-1 text-xs font-semibold rounded-full ';
    switch (status) {
      case 'ASSIGNED':
        return classes + 'bg-blue-100 text-blue-800';
      case 'UNDER_REVIEW':
        return classes + 'bg-yellow-100 text-yellow-800';
      case 'APPROVED':
        return classes + 'bg-green-100 text-green-800';
      case 'REJECTED':
        return classes + 'bg-red-100 text-red-800';
      default:
        return classes + 'bg-gray-100 text-gray-800';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'ASSIGNED': return 'Assigned';
      case 'UNDER_REVIEW': return 'Under Review';
      case 'APPROVED': return 'Approved';
      case 'REJECTED': return 'Rejected';
      default: return status;
    }
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-IN', { 
      year: 'numeric', 
      month: 'short', 
      day: 'numeric' 
    });
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', {
      maximumFractionDigits: 0
    }).format(amount);
  }
}
