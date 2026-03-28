import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UnderwriterService } from '../../services/underwriter.service';

@Component({
  selector: 'app-uw-claim-inspections',
  standalone: true,
  imports: [CommonModule],
  template: `
<div class="content-container min-h-screen py-8">
  <div class="flex items-center justify-between mb-8">
    <div>
      <h1 class="page-title text-2xl font-bold text-gray-900 line-height-tight">Claim Inspection Tracking</h1>
      <p class="text-gray-600 mt-1">Monitor all claim damage assessments and field investigator statuses</p>
    </div>
    <button (click)="loadInspections()" class="flex items-center gap-2 px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors font-medium">
      <span class="material-icons text-lg">refresh</span>
      Refresh
    </button>
  </div>

  <div class="flex flex-wrap gap-2 mb-6 bg-white rounded-xl p-2 shadow-sm border border-gray-200 w-fit">
    @for (filter of ['ALL', 'ASSIGNED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED']; track filter) {
      <button (click)="applyFilter(filter)"
        class="px-4 py-2 rounded-lg text-sm font-medium transition-all"
        [class]="activeFilter() === filter ? 'bg-fire-orange text-white shadow-sm' : 'text-gray-600 hover:bg-gray-100'">
        {{ filter.replace('_', ' ') }}
      </button>
    }
  </div>

  @if (isLoading()) {
    <div class="flex items-center justify-center py-16">
      <div class="w-10 h-10 border-4 border-gray-200 border-t-fire-orange rounded-full animate-spin"></div>
      <p class="ml-3 text-gray-600">Loading claim inspections...</p>
    </div>
  }

  @if (!isLoading()) {
    <div class="bg-white rounded-xl shadow-md border border-gray-200 overflow-hidden">
      <div class="px-6 py-4 border-b border-gray-200 bg-gray-50/50">
        <p class="text-sm text-gray-600">
          Current Inspections: <span class="font-semibold text-gray-900">{{ filteredInspections().length }}</span>
        </p>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead>
            <tr class="bg-gray-50 border-b border-gray-200">
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">ID</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Claim & Customer</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Surveyor</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Claimed Amount</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Assessed Loss</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Status</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Report Date</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            @if (filteredInspections().length === 0) {
              <tr>
                <td colspan="7" class="px-6 py-16 text-center">
                  <span class="material-icons text-5xl text-gray-300 mb-3 block">search_check</span>
                  <p class="text-gray-500 font-medium">No claim inspections found</p>
                  <p class="text-gray-400 text-sm mt-1">Once you assign a surveyor to a claim, it will appear here for tracking</p>
                </td>
              </tr>
            }
            @for (ci of filteredInspections(); track ci.inspectionId) {
              <tr class="hover:bg-gray-50/50 transition-colors">
                <td class="px-6 py-4">
                  <div class="flex items-center gap-2">
                    <div class="w-8 h-8 bg-orange-50 rounded-lg flex items-center justify-center">
                      <span class="material-icons text-orange-500 text-sm">local_fire_department</span>
                    </div>
                    <span class="text-sm font-bold text-gray-900">#CI-{{ ci.inspectionId }}</span>
                  </div>
                </td>
                <td class="px-6 py-4 text-sm font-medium text-gray-700">
                  <div class="text-gray-900 font-bold mb-0.5">{{ ci.customerName || 'N/A' }}</div>
                  <div class="text-xs text-blue-600">CLAIM #{{ ci.claimId }}</div>
                </td>
                <td class="px-6 py-4 text-sm">
                   <div class="flex items-center gap-2 italic text-gray-600">
                    <span class="material-icons text-xs">gavel</span>
                    <span>{{ ci.surveyorName || 'Unknown' }}</span>
                  </div>
                </td>
                <td class="px-6 py-4 text-sm text-gray-700 font-semibold">
                  @if (ci.requestedClaimAmount != null) {
                    <span>₹{{ getPolicyCoverageAmount(ci.requestedClaimAmount) }}</span>
                  } @else { <span class="text-gray-400">—</span> }
                </td>
                <td class="px-6 py-4 text-sm font-bold text-green-700">
                  @if (ci.estimatedLoss != null) {
                    <span>₹{{ getPolicyCoverageAmount(ci.estimatedLoss) }}</span>
                  } @else { <span class="text-gray-400 font-medium">—</span> }
                </td>
                <td class="px-6 py-4">
                  <span class="px-2.5 py-1 text-xs font-semibold rounded-full" [ngClass]="getStatusClass(ci.status)">
                    {{ ci.status.replace('_', ' ') }}
                  </span>
                </td>
                <td class="px-6 py-4 text-sm text-gray-500">
                  {{ ci.inspectionDate ? (ci.inspectionDate | date:'mediumDate') : 'Pending' }}
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </div>
  }
</div>
  `
})
export class UwClaimInspectionsComponent implements OnInit {
  private underwriterService = inject(UnderwriterService);

  inspections = signal<any[]>([]);
  filteredInspections = signal<any[]>([]);
  isLoading = signal<boolean>(true);
  activeFilter = signal<string>('ALL');

  ngOnInit(): void { this.loadInspections(); }

  loadInspections(): void {
    this.isLoading.set(true);
    this.underwriterService.getAllClaimInspections().subscribe({
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
      'UNDER_REVIEW': 'bg-yellow-100 text-yellow-800',
      'APPROVED': 'bg-green-100 text-green-800',
      'REJECTED': 'bg-red-100 text-red-800'
    };
    return map[status] || 'bg-gray-100 text-gray-800';
  }

  getPolicyCoverageAmount(amount: number | null | undefined): string {
    if (!amount) return '—';
    return amount.toLocaleString('en-IN');
  }
}
