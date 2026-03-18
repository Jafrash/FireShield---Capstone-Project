import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { SurveyorService } from '../../services/surveyor.service';
import { DocumentService } from '../../../../core/services/document.service';
import { InspectionDocumentSummary, PropertyInspection } from '../../../../core/models/inspection.model';
import { InspectionDocumentUploadComponent } from '../../../../shared/components/ui/inspection-document-upload/inspection-document-upload.component';
import { CustomValidators } from '../../../../shared/validators/custom-validators';
import { ValidationMessages } from '../../../../shared/helpers/validation-messages';

@Component({
  selector: 'app-property-inspections',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, InspectionDocumentUploadComponent],
  template: `
<div class="content-container min-h-screen py-8">
  <div class="flex items-center justify-between mb-8">
    <div>
      <h1 class="page-title">Property Inspections</h1>
      <p class="text-gray-600 mt-1">View assigned property inspections and submit your reports</p>
    </div>
    <button (click)="loadInspections()" class="flex items-center gap-2 px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors font-medium">
      <span class="material-icons text-lg">refresh</span>
      Refresh
    </button>
  </div>

  @if (successMessage()) {
    <div class="mb-6 flex items-center gap-3 bg-green-50 border border-green-200 text-green-800 px-4 py-3 rounded-lg">
      <span class="material-icons text-green-600">check_circle</span>
      {{ successMessage() }}
    </div>
  }
  @if (errorMessage() && !showReportModal()) {
    <div class="mb-6 flex items-center gap-3 bg-red-50 border border-red-200 text-red-800 px-4 py-3 rounded-lg">
      <span class="material-icons text-red-600">error</span>
      {{ errorMessage() }}
    </div>
  }

  <div class="flex gap-2 mb-6 bg-white rounded-xl p-2 shadow-sm border border-gray-200 w-fit">
    @for (filter of ['ALL', 'ASSIGNED', 'COMPLETED', 'REJECTED']; track filter) {
      <button (click)="applyFilter(filter)"
        class="px-4 py-2 rounded-lg text-sm font-medium transition-all"
        [class]="activeFilter() === filter ? 'bg-[#8B1E3F] text-white shadow-sm' : 'text-gray-600 hover:bg-gray-100'">
        {{ filter }}
      </button>
    }
  </div>

  @if (isLoading()) {
    <div class="flex items-center justify-center py-16">
      <div class="w-10 h-10 border-4 border-gray-200 border-t-[#8B1E3F] rounded-full animate-spin"></div>
      <p class="ml-3 text-gray-600">Loading inspections...</p>
    </div>
  }

  @if (!isLoading()) {
    <div class="bg-white rounded-xl shadow-md border border-gray-200">
      <div class="px-6 py-4 border-b border-gray-200">
        <p class="text-sm text-gray-600">
          Showing <span class="font-semibold text-gray-900">{{ filteredInspections().length }}</span> of
          <span class="font-semibold text-gray-900">{{ inspections().length }}</span> inspections
        </p>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead>
            <tr class="bg-gray-50 border-b border-gray-200">
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Inspection ID</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Property ID</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Customer</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Property</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Customer Docs</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Inspection Date</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Risk Score</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Status</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Action</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            @if (filteredInspections().length === 0) {
              <tr>
                <td colspan="9" class="px-6 py-16 text-center">
                  <span class="material-icons text-5xl text-gray-300 mb-3 block">home_work</span>
                  <p class="text-gray-500 font-medium">No inspections found</p>
                  <p class="text-gray-400 text-sm mt-1">Check back when the admin assigns inspections to you</p>
                </td>
              </tr>
            }
            @for (insp of filteredInspections(); track insp.inspectionId) {
              <tr class="hover:bg-gray-50 transition-colors">
                <td class="px-6 py-4 text-sm font-bold text-gray-900">#INS-{{ insp.inspectionId }}</td>
                <td class="px-6 py-4 text-sm text-gray-700">
                  <div class="flex items-center gap-2">
                    <span class="material-icons text-blue-500 text-lg">home</span>
                    #PROP-{{ insp.propertyId }}
                  </div>
                </td>
                <td class="px-6 py-4 text-sm text-gray-700">
                  <div class="font-semibold text-gray-900">{{ insp.customerName || 'N/A' }}</div>
                  <div class="text-xs text-gray-500">{{ insp.customerEmail || 'No email' }}</div>
                  <div class="text-xs text-gray-500">{{ insp.customerPhone || 'No phone' }}</div>
                </td>
                <td class="px-6 py-4 text-sm text-gray-700">
                  <div class="font-medium text-gray-900">{{ insp.propertyType || 'N/A' }}</div>
                  <div class="text-xs text-gray-500">{{ insp.propertyAddress || 'No address' }}</div>
                </td>
                <td class="px-6 py-4 text-sm">
                  @if ((insp.customerDocuments?.length || 0) > 0) {
                    <div class="flex flex-col gap-1">
                      @for (doc of insp.customerDocuments!.slice(0, 2); track doc.documentId) {
                        <button type="button" (click)="downloadCustomerDocument(doc)" class="text-left text-xs text-blue-700 hover:text-blue-900 hover:underline">
                          {{ doc.fileName }}
                        </button>
                      }
                      @if ((insp.customerDocuments?.length || 0) > 2) {
                        <span class="text-xs text-gray-500">+{{ (insp.customerDocuments?.length || 0) - 2 }} more</span>
                      }
                    </div>
                  } @else {
                    <span class="text-xs text-gray-400">No customer docs</span>
                  }
                </td>
                <td class="px-6 py-4 text-sm text-gray-600">
                  {{ insp.inspectionDate ? (insp.inspectionDate | date:'mediumDate') : '—' }}
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
                <td class="px-6 py-4">
                  @if (insp.status === 'ASSIGNED') {
                    <button (click)="openReportModal(insp)"
                      class="flex items-center gap-1.5 px-4 py-2 bg-[#8B1E3F] text-white text-sm rounded-lg hover:bg-[#6f1732] transition-colors font-medium">
                      <span class="material-icons text-sm">upload_file</span>
                      Submit Report
                    </button>
                  } @else {
                    <span class="text-xs text-gray-400 italic">Report submitted</span>
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </div>
  }
</div>

@if (showReportModal() && selectedInspection()) {
  <div class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4" (click)="closeReportModal()">
    <div class="bg-white rounded-xl shadow-2xl w-full max-w-lg" (click)="$event.stopPropagation()">
      <div class="bg-gradient-to-r from-[#8B1E3F] to-[#6f1732] px-6 py-4 rounded-t-xl">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="p-2 bg-white/20 rounded-lg"><span class="material-icons text-white">home_work</span></div>
            <div>
              <h2 class="text-white font-bold text-lg">Submit Inspection Report</h2>
              <p class="text-white/80 text-sm">Inspection #INS-{{ selectedInspection()!.inspectionId }}</p>
            </div>
          </div>
          <button (click)="closeReportModal()" class="text-white/70 hover:text-white"><span class="material-icons">close</span></button>
        </div>
      </div>
      <div class="p-6">
        <form [formGroup]="reportForm" class="space-y-5">
          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-1.5">Risk Score <span class="text-red-500">*</span> <span class="font-normal text-gray-500">(0 = Low, 10 = High)</span></label>
            <input type="number" formControlName="assessedRiskScore" step="0.1" min="0" max="10"
              class="w-full px-4 py-3 bg-gray-50 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#8B1E3F] focus:bg-white transition-colors"
              placeholder="e.g. 3.5" />
            @if (reportForm.get('assessedRiskScore')?.touched && reportForm.get('assessedRiskScore')?.invalid) {
              <p class="text-red-500 text-xs mt-1">Risk score between 0 and 10 is required.</p>
            }
          </div>
          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-1.5">Inspection Remarks <span class="text-red-500">*</span></label>
            <textarea formControlName="remarks" rows="5"
              class="w-full px-4 py-3 bg-gray-50 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#8B1E3F] focus:bg-white transition-colors resize-none"
              placeholder="Describe the property condition, fire safety measures observed, and overall assessment..."></textarea>
            @if (reportForm.get('remarks')?.touched && reportForm.get('remarks')?.invalid) {
              <p class="text-red-500 text-xs mt-1">Remarks must be at least 20 characters.</p>
            }
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            <label class="flex items-center gap-2 text-sm text-gray-700"><input type="checkbox" formControlName="fireSafetyAvailable"> Fire safety available</label>
            <label class="flex items-center gap-2 text-sm text-gray-700"><input type="checkbox" formControlName="sprinklerSystem"> Sprinkler system available</label>
            <label class="flex items-center gap-2 text-sm text-gray-700"><input type="checkbox" formControlName="fireExtinguishers"> Fire extinguishers available</label>
            <div>
              <label class="block text-xs font-semibold text-gray-600 mb-1">Distance from Fire Station (km)</label>
              <input type="number" formControlName="distanceFromFireStation" class="w-full px-3 py-2 bg-gray-50 border border-gray-300 rounded-lg" min="0" step="0.1">
            </div>
            <div>
              <label class="block text-xs font-semibold text-gray-600 mb-1">Construction Risk (0-1)</label>
              <input type="number" formControlName="constructionRisk" class="w-full px-3 py-2 bg-gray-50 border border-gray-300 rounded-lg" min="0" max="1" step="0.1">
            </div>
            <div>
              <label class="block text-xs font-semibold text-gray-600 mb-1">Hazard Risk (0-1)</label>
              <input type="number" formControlName="hazardRisk" class="w-full px-3 py-2 bg-gray-50 border border-gray-300 rounded-lg" min="0" max="1" step="0.1">
            </div>
            <div>
              <label class="block text-xs font-semibold text-gray-600 mb-1">Recommended Coverage (₹)</label>
              <input type="number" formControlName="recommendedCoverage" class="w-full px-3 py-2 bg-gray-50 border border-gray-300 rounded-lg" min="0" step="1000">
            </div>
            <div>
              <label class="block text-xs font-semibold text-gray-600 mb-1">Recommended Premium (₹)</label>
              <input type="number" formControlName="recommendedPremium" class="w-full px-3 py-2 bg-gray-50 border border-gray-300 rounded-lg" min="0" step="100">
            </div>
          </div>
          @if (errorMessage()) {
            <div class="flex gap-2 bg-red-50 border border-red-200 text-red-700 px-3 py-2 rounded-lg text-sm">
              <span class="material-icons text-sm mt-0.5">error</span>{{ errorMessage() }}
            </div>
          }
          
          <div class="mt-4">
            <app-inspection-document-upload
              [inspectionId]="selectedInspection()!.inspectionId"
              [isClaimInspection]="false"
            ></app-inspection-document-upload>
          </div>
        </form>
      </div>
      <div class="px-6 pb-6 flex gap-3 justify-end">
        <button type="button" (click)="closeReportModal()" class="px-5 py-2.5 text-gray-700 font-medium bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors">Cancel</button>
        <button type="button" (click)="submitReport()" [disabled]="isSubmitting() || reportForm.invalid"
          class="flex items-center gap-2 px-5 py-2.5 bg-[#8B1E3F] text-white font-medium rounded-lg hover:bg-[#6f1732] transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
          @if (isSubmitting()) { <span class="material-icons animate-spin text-sm">autorenew</span> } @else { <span class="material-icons text-sm">check_circle</span> }
          {{ isSubmitting() ? 'Submitting...' : 'Submit Report' }}
        </button>
      </div>
    </div>
  </div>
}
  `
})
export class PropertyInspectionsComponent implements OnInit {
  private surveyorService = inject(SurveyorService);
  private documentService = inject(DocumentService);
  private fb = inject(FormBuilder);

  inspections = signal<PropertyInspection[]>([]);
  filteredInspections = signal<PropertyInspection[]>([]);
  isLoading = signal<boolean>(true);
  isSubmitting = signal<boolean>(false);
  errorMessage = signal<string>('');
  successMessage = signal<string>('');
  selectedInspection = signal<PropertyInspection | null>(null);
  showReportModal = signal<boolean>(false);
  activeFilter = signal<string>('ALL');

  reportForm: FormGroup = this.fb.group({
    assessedRiskScore: [null, [Validators.required, Validators.min(0), Validators.max(10)]],
    remarks: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(2000), CustomValidators.noWhitespace()]],
    fireSafetyAvailable: [false],
    sprinklerSystem: [false],
    fireExtinguishers: [false],
    distanceFromFireStation: [null, [Validators.min(0)]],
    constructionRisk: [null, [Validators.min(0), Validators.max(1)]],
    hazardRisk: [null, [Validators.min(0), Validators.max(1)]],
    recommendedCoverage: [null, [Validators.min(0)]],
    recommendedPremium: [null, [Validators.min(0)]]
  });

  ngOnInit(): void { this.loadInspections(); }

  loadInspections(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.surveyorService.getMyPropertyInspections().subscribe({
      next: (data) => { this.inspections.set(data); this.applyFilter(this.activeFilter()); this.isLoading.set(false); },
      error: (err) => { console.error(err); this.errorMessage.set('Failed to load inspections.'); this.isLoading.set(false); }
    });
  }

  applyFilter(filter: string): void {
    this.activeFilter.set(filter);
    this.filteredInspections.set(filter === 'ALL' ? this.inspections() : this.inspections().filter(i => i.status === filter));
  }

  openReportModal(inspection: PropertyInspection): void {
    this.selectedInspection.set(inspection);
    this.reportForm.reset({
      assessedRiskScore: null,
      remarks: '',
      fireSafetyAvailable: false,
      sprinklerSystem: false,
      fireExtinguishers: false,
      distanceFromFireStation: null,
      constructionRisk: null,
      hazardRisk: null,
      recommendedCoverage: null,
      recommendedPremium: null
    });
    this.showReportModal.set(true);
    this.errorMessage.set('');
  }

  closeReportModal(): void { this.showReportModal.set(false); this.selectedInspection.set(null); this.reportForm.reset(); }

  submitReport(): void {
    if (this.reportForm.invalid) { this.reportForm.markAllAsTouched(); return; }
    const insp = this.selectedInspection();
    if (!insp) return;
    this.isSubmitting.set(true);
    this.surveyorService.submitPropertyInspectionReport(insp.inspectionId, this.reportForm.value).subscribe({
      next: (updated) => {
        this.inspections.set(this.inspections().map(i => i.inspectionId === updated.inspectionId ? updated : i));
        this.applyFilter(this.activeFilter());
        this.isSubmitting.set(false);
        this.closeReportModal();
        this.successMessage.set('Inspection report submitted successfully!');
        setTimeout(() => this.successMessage.set(''), 4000);
      },
      error: (err) => { console.error(err); this.isSubmitting.set(false); this.errorMessage.set('Failed to submit report.'); }
    });
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = { 'ASSIGNED': 'bg-blue-100 text-blue-800', 'COMPLETED': 'bg-green-100 text-green-800', 'REJECTED': 'bg-red-100 text-red-800' };
    return map[status] || 'bg-gray-100 text-gray-800';
  }

  getRiskClass(score: number | null): string {
    if (score === null) return 'text-gray-400';
    if (score <= 3) return 'text-green-600 font-semibold';
    if (score <= 6) return 'text-yellow-600 font-semibold';
    return 'text-red-600 font-semibold';
  }

  getErrorMessage(controlName: string): string {
    const field = this.reportForm.get(controlName);
    if (field && field.invalid && (field.dirty || field.touched)) {
      return ValidationMessages.getErrorMessage(controlName, field.errors);
    }
    return '';
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.reportForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  isFieldValid(fieldName: string): boolean {
    const field = this.reportForm.get(fieldName);
    return !!(field && field.valid && field.dirty);
  }

  downloadCustomerDocument(doc: InspectionDocumentSummary): void {
    this.documentService.downloadDocument(doc.documentId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const anchor = window.document.createElement('a');
        anchor.href = url;
        anchor.download = doc.fileName || 'customer-document';
        window.document.body.appendChild(anchor);
        anchor.click();
        window.document.body.removeChild(anchor);
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Failed to download customer document', err);
        this.errorMessage.set('Failed to download customer document.');
      }
    });
  }
}
