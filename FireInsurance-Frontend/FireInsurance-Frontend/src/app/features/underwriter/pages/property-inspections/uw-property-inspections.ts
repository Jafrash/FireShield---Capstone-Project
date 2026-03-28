import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UnderwriterService } from '../../services/underwriter.service';
import { DocumentService } from '../../../../core/services/document.service';

@Component({
  selector: 'app-uw-property-inspections',
  standalone: true,
  imports: [CommonModule],
  template: `
<div class="content-container min-h-screen py-8">
  <div class="flex items-center justify-between mb-8">
    <div>
      <h1 class="page-title text-2xl font-bold text-gray-900 line-height-tight">Property Inspection Tracking</h1>
      <p class="text-gray-600 mt-1">Track the status of all property inspections you have assigned to surveyors</p>
    </div>
    <button (click)="loadInspections()" class="flex items-center gap-2 px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors font-medium">
      <span class="material-icons text-lg">refresh</span>
      Refresh
    </button>
  </div>

  <div class="flex gap-2 mb-6 bg-white rounded-xl p-2 shadow-sm border border-gray-200 w-fit">
    @for (filter of ['ALL', 'ASSIGNED', 'COMPLETED', 'REJECTED']; track filter) {
      <button (click)="applyFilter(filter)"
        class="px-4 py-2 rounded-lg text-sm font-medium transition-all"
        [class]="activeFilter() === filter ? 'bg-fire-orange text-white shadow-sm' : 'text-gray-600 hover:bg-gray-100'">
        {{ filter }}
      </button>
    }
  </div>

  @if (isLoading()) {
    <div class="flex items-center justify-center py-16">
      <div class="w-10 h-10 border-4 border-gray-200 border-t-fire-orange rounded-full animate-spin"></div>
      <p class="ml-3 text-gray-600">Loading inspections...</p>
    </div>
  }

  @if (!isLoading()) {
    <div class="bg-white rounded-xl shadow-md border border-gray-200 overflow-hidden">
      <div class="px-6 py-4 border-b border-gray-200 bg-gray-50/50">
        <p class="text-sm text-gray-600">
          Total Inspections: <span class="font-semibold text-gray-900">{{ filteredInspections().length }}</span>
        </p>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead>
            <tr class="bg-gray-50 border-b border-gray-200">
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">ID</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Property & Customer</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Assigned Surveyor</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Requested Coverage</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Risk Score</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Status</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Last Sync</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Action</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            @if (filteredInspections().length === 0) {
              <tr>
                <td colspan="8" class="px-6 py-16 text-center">
                  <span class="material-icons text-5xl text-gray-300 mb-3 block">home_work</span>
                  <p class="text-gray-500 font-medium">No inspections to display</p>
                  <p class="text-gray-400 text-sm mt-1">Inspections will appear here once you assign surveyors to policy applications</p>
                </td>
              </tr>
            }
            @for (insp of filteredInspections(); track insp.inspectionId) {
              <tr class="hover:bg-gray-50/50 transition-colors">
                <td class="px-6 py-4">
                  <span class="text-sm font-bold text-gray-900">#INS-{{ insp.inspectionId }}</span>
                </td>
                <td class="px-6 py-4 text-sm">
                  <div class="font-semibold text-gray-900">{{ insp.customerName || 'N/A' }}</div>
                  <div class="text-xs text-gray-500">{{ insp.propertyType || 'N/A' }}</div>
                  <div class="text-xs text-blue-600 font-medium mt-0.5">#PROP-{{ insp.propertyId }}</div>
                </td>
                <td class="px-6 py-4 text-sm">
                  <div class="flex items-center gap-2">
                    <div class="w-7 h-7 bg-orange-100 rounded-full flex items-center justify-center">
                      <span class="material-icons text-orange-600 text-xs text-sm">person</span>
                    </div>
                    <span>{{ insp.surveyorName || 'Unassigned' }}</span>
                  </div>
                </td>
                <td class="px-6 py-4 text-sm text-gray-700">
                   <div class="font-semibold text-gray-900">₹{{ getPolicyCoverageAmount(insp.requestedSumInsured) }}</div>
                </td>
                <td class="px-6 py-4 text-sm">
                  <span [class]="getRiskClass(insp.assessedRiskScore)">
                    {{ insp.assessedRiskScore != null ? (insp.assessedRiskScore + '/10') : '—' }}
                  </span>
                </td>
                <td class="px-6 py-4">
                  <span class="px-2.5 py-1 text-xs font-semibold rounded-full" [ngClass]="getStatusClass(insp.status)">
                    {{ insp.status }}
                  </span>
                </td>
                <td class="px-6 py-4 text-sm text-gray-500">
                  {{ insp.inspectionDate ? (insp.inspectionDate | date:'shortDate') : 'Pending' }}
                </td>
                <td class="px-6 py-4">
                  <button (click)="openDetailModal(insp)" class="text-blue-600 hover:text-blue-800 text-sm font-semibold flex items-center gap-1">
                    <span class="material-icons text-sm">visibility</span>
                    View Report
                  </button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </div>
  }
</div>

@if (showDetailModal() && selectedInspection()) {
  <div class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4" (click)="closeDetailModal()">
    <div class="bg-white rounded-xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-y-auto" (click)="$event.stopPropagation()">
      <div class="bg-fire-charcoal px-6 py-4 sticky top-0 z-10">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="p-2 bg-fire-orange rounded-lg">
              <span class="material-icons text-white">analytics</span>
            </div>
            <div>
              <h2 class="text-white font-bold text-lg">Technical Risk Profile (COPE)</h2>
              <p class="text-gray-400 text-sm font-medium">Inspection #INS-{{ selectedInspection()!.inspectionId }} • {{ selectedInspection()!.status }}</p>
            </div>
          </div>
          <button (click)="closeDetailModal()" class="text-gray-400 hover:text-white transition-colors">
            <span class="material-icons">close</span>
          </button>
        </div>
      </div>

      <div class="p-6 space-y-8">
        <!-- Executive Summary -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div class="p-4 bg-gray-50 rounded-xl border border-gray-100 text-center">
            <span class="text-xs uppercase text-gray-500 font-bold tracking-wider">Risk Score</span>
            <div [class]="getRiskClass(selectedInspection()!.assessedRiskScore) + ' text-3xl mt-1'">
              {{ selectedInspection()!.assessedRiskScore != null ? selectedInspection()!.assessedRiskScore + '/10' : 'N/A' }}
            </div>
          </div>
          <div class="p-4 bg-gray-50 rounded-xl border border-gray-100 text-center">
            <span class="text-xs uppercase text-gray-500 font-bold tracking-wider">Rec. Coverage</span>
            <div class="text-gray-900 text-xl font-bold mt-1">
              ₹{{ getPolicyCoverageAmount(selectedInspection()!.recommendedCoverage) }}
            </div>
          </div>
          <div class="p-4 bg-gray-50 rounded-xl border border-gray-100 text-center">
            <span class="text-xs uppercase text-gray-500 font-bold tracking-wider">Rec. Premium</span>
            <div class="text-fire-orange text-xl font-bold mt-1">
              ₹{{ getPolicyCoverageAmount(selectedInspection()!.recommendedPremium) }}
            </div>
          </div>
        </div>

        <!-- COPE Details -->
        <div class="space-y-4">
          <h3 class="text-sm font-bold text-gray-900 flex items-center gap-2">
            <span class="material-icons text-lg text-fire-orange">domain</span>
            Property Attributes & COPE Factors
          </h3>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-4 bg-gray-50/50 p-6 rounded-xl border border-gray-100">
            <div class="space-y-3">
              <div class="flex justify-between border-b border-gray-100 pb-1.5">
                <span class="text-sm text-gray-500">Construction</span>
                <span class="text-sm font-semibold text-gray-900">{{ selectedInspection()!.constructionType || 'Not Specified' }}</span>
              </div>
              <div class="flex justify-between border-b border-gray-100 pb-1.5">
                <span class="text-sm text-gray-500">Roofing</span>
                <span class="text-sm font-semibold text-gray-900">{{ selectedInspection()!.roofType || 'Not Specified' }}</span>
              </div>
              <div class="flex justify-between border-b border-gray-100 pb-1.5">
                <span class="text-sm text-gray-500">Occupancy Tier</span>
                <span class="text-sm font-semibold text-gray-900">{{ selectedInspection()!.occupancyType || 'Not Specified' }}</span>
              </div>
              <div class="flex justify-between border-b border-gray-100 pb-1.5">
                <span class="text-sm text-gray-500">Electrical Audit</span>
                <span [class]="selectedInspection()!.electricalAuditStatus === 'PASS' ? 'text-green-600' : 'text-red-500'" class="text-sm font-bold">
                  {{ selectedInspection()!.electricalAuditStatus || 'PENDING' }}
                </span>
              </div>
            </div>
            <div class="space-y-3">
              <div class="flex justify-between border-b border-gray-100 pb-1.5">
                <span class="text-sm text-gray-500">Nearest Station</span>
                <span class="text-sm font-semibold text-gray-900">{{ selectedInspection()!.distanceFromFireStation || '0' }} km</span>
              </div>
              <div class="flex justify-between border-b border-gray-100 pb-1.5">
                <span class="text-sm text-gray-500">Adj. Bldg Distance</span>
                <span class="text-sm font-semibold text-gray-900">{{ selectedInspection()!.adjacentBuildingDistance || 'N/A' }} m</span>
              </div>
              <div class="flex justify-between border-b border-gray-100 pb-1.5">
                <span class="text-sm text-gray-500">Hazardous Materials</span>
                <span class="text-sm font-semibold" [class]="selectedInspection()!.hazardousMaterialsPresent ? 'text-red-500' : 'text-green-600'">
                  {{ selectedInspection()!.hazardousMaterialsPresent ? 'YES (Critical)' : 'NO' }}
                </span>
              </div>
              <div class="flex justify-between border-b border-gray-100 pb-1.5">
                <span class="text-sm text-gray-500">Sprinkler System</span>
                <span class="text-sm font-semibold" [class]="selectedInspection()!.sprinklerSystem ? 'text-green-600' : 'text-gray-400'">
                  {{ selectedInspection()!.sprinklerSystem ? 'YES' : 'NO' }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Surveyor Analysis -->
        <div class="space-y-4">
          <h3 class="text-sm font-bold text-gray-900 flex items-center gap-2">
            <span class="material-icons text-lg text-fire-orange">rate_review</span>
            Surveyor Observations & Analysis
          </h3>
          <div class="p-6 bg-white border border-gray-200 rounded-xl space-y-4">
            <div>
              <p class="text-xs font-bold text-gray-400 uppercase tracking-widest mb-1.5">Internal Protection & Safety Notes</p>
              <p class="text-sm text-gray-700 leading-relaxed italic">
                {{ selectedInspection()!.internalProtectionNotes || 'No specific protection notes provided.' }}
              </p>
            </div>
            <div>
              <p class="text-xs font-bold text-gray-400 uppercase tracking-widest mb-1.5">General Remarks</p>
              <div class="text-sm text-gray-700 leading-relaxed whitespace-pre-wrap">
                {{ selectedInspection()!.remarks || 'No remarks provided.' }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="px-6 py-5 border-t border-gray-100 flex justify-end">
        <button (click)="closeDetailModal()" class="px-6 py-2 bg-gray-900 text-white font-bold rounded-lg hover:bg-fire-charcoal transition-colors shadow-lg shadow-gray-200">
          Done Reviewing
        </button>
      </div>
    </div>
  </div>
}
  `
})
export class UwPropertyInspectionsComponent implements OnInit {
  private underwriterService = inject(UnderwriterService);

  inspections = signal<any[]>([]);
  filteredInspections = signal<any[]>([]);
  isLoading = signal<boolean>(true);
  activeFilter = signal<string>('ALL');
  
  selectedInspection = signal<any | null>(null);
  showDetailModal = signal<boolean>(false);

  ngOnInit(): void { this.loadInspections(); }
  
  openDetailModal(inspection: any): void {
    this.selectedInspection.set(inspection);
    this.showDetailModal.set(true);
  }

  closeDetailModal(): void {
    this.showDetailModal.set(false);
    this.selectedInspection.set(null);
  }

  loadInspections(): void {
    this.isLoading.set(true);
    this.underwriterService.getAllPropertyInspections().subscribe({
      next: (data) => {
        this.inspections.set(data);
        this.applyFilter(this.activeFilter());
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error(err);
        this.isLoading.set(false);
      }
    });
  }

  applyFilter(filter: string): void {
    this.activeFilter.set(filter);
    this.filteredInspections.set(filter === 'ALL' ? this.inspections() : this.inspections().filter(i => i.status === filter));
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      'ASSIGNED': 'bg-blue-100 text-blue-800',
      'COMPLETED': 'bg-green-100 text-green-800',
      'REJECTED': 'bg-red-100 text-red-800'
    };
    return map[status] || 'bg-gray-100 text-gray-800';
  }

  getRiskClass(score: number | null): string {
    if (score === null) return 'text-gray-400 font-medium italic';
    if (score <= 3) return 'text-green-600 font-bold';
    if (score <= 6) return 'text-yellow-600 font-bold';
    return 'text-red-600 font-bold';
  }

  getPolicyCoverageAmount(amount: number | null | undefined): string {
    if (!amount) return '—';
    return amount.toLocaleString('en-IN');
  }
}
