import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { SurveyorService } from '../../services/surveyor.service';
import { DocumentService } from '../../../../core/services/document.service';
import { ClaimInspectionItem, InspectionDocumentSummary } from '../../../../core/models/inspection.model';
import { InspectionDocumentUploadComponent } from '../../../../shared/components/ui/inspection-document-upload/inspection-document-upload.component';
import { CustomValidators } from '../../../../shared/validators/custom-validators';
import { ValidationMessages } from '../../../../shared/helpers/validation-messages';

@Component({
  selector: 'app-claim-inspections',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, InspectionDocumentUploadComponent],
  template: `
<div class="content-container min-h-screen py-8">
  <div class="flex items-center justify-between mb-8">
    <div>
      <h1 class="page-title">Claim Inspections</h1>
      <p class="text-gray-600 mt-1">Verify fire damage claims and submit your investigation reports</p>
    </div>
    <button (click)="loadInspections()" class="flex items-center gap-2 px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors font-medium">
      <span class="material-icons text-lg">refresh</span>Refresh
    </button>
  </div>

  @if (successMessage()) {
    <div class="mb-6 flex items-center gap-3 bg-green-50 border border-green-200 text-green-800 px-4 py-3 rounded-lg">
      <span class="material-icons text-green-600">check_circle</span>{{ successMessage() }}
    </div>
  }
  @if (errorMessage() && !showReportModal()) {
    <div class="mb-6 flex items-center gap-3 bg-red-50 border border-red-200 text-red-800 px-4 py-3 rounded-lg">
      <span class="material-icons text-red-600">error</span>{{ errorMessage() }}
    </div>
  }

  <div class="flex flex-wrap gap-2 mb-6 bg-white rounded-xl p-2 shadow-sm border border-gray-200 w-fit">
    @for (filter of ['ALL', 'ASSIGNED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED']; track filter) {
      <button (click)="applyFilter(filter)" class="px-4 py-2 rounded-lg text-sm font-medium transition-all"
        [class]="activeFilter() === filter ? 'bg-[#C72B32] text-white shadow-sm' : 'text-gray-600 hover:bg-gray-100'">
        {{ filter.replace('_', ' ') }}
      </button>
    }
  </div>

  @if (isLoading()) {
    <div class="flex items-center justify-center py-16">
      <div class="w-10 h-10 border-4 border-gray-200 border-t-[#C72B32] rounded-full animate-spin"></div>
      <p class="ml-3 text-gray-600">Loading claim inspections...</p>
    </div>
  }

  @if (!isLoading()) {
    <div class="bg-white rounded-xl shadow-md border border-gray-200">
      <div class="px-6 py-4 border-b border-gray-200">
        <p class="text-sm text-gray-600">
          Showing <span class="font-semibold text-gray-900">{{ filteredInspections().length }}</span> of
          <span class="font-semibold text-gray-900">{{ inspections().length }}</span> claim inspections
        </p>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead>
            <tr class="bg-gray-50 border-b border-gray-200">
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Inspection ID</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Claim ID</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Customer</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Requested Amount</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Policy & Coverage</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Customer Docs</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Inspection Date</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Estimated Loss</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Status</th>
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Action</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            @if (filteredInspections().length === 0) {
              <tr>
                <td colspan="10" class="px-6 py-16 text-center">
                  <span class="material-icons text-5xl text-gray-300 mb-3 block">assignment_turned_in</span>
                  <p class="text-gray-500 font-medium">No claim inspections found</p>
                </td>
              </tr>
            }
            @for (ci of filteredInspections(); track ci.inspectionId) {
              <tr class="hover:bg-gray-50 transition-colors">
                <td class="px-6 py-4">
                  <div class="flex items-center gap-2">
                    <div class="w-8 h-8 bg-orange-50 rounded-lg flex items-center justify-center">
                      <span class="material-icons text-orange-500 text-sm">local_fire_department</span>
                    </div>
                    <span class="text-sm font-bold text-gray-900">#CI-{{ ci.inspectionId }}</span>
                  </div>
                </td>
                <td class="px-6 py-4 text-sm font-medium text-gray-700">#CLM-{{ ci.claimId }}</td>
                <td class="px-6 py-4 text-sm text-gray-700">
                  <div class="font-semibold text-gray-900">{{ ci.customerName || 'N/A' }}</div>
                  <div class="text-xs text-gray-500">{{ ci.customerEmail || 'No email' }}</div>
                  <div class="text-xs text-gray-500">{{ ci.customerPhone || 'No phone' }}</div>
                </td>
                <td class="px-6 py-4 text-sm text-gray-700">
                  @if (ci.requestedClaimAmount != null) {
                    <span class="font-semibold text-blue-700">₹{{ getPolicyCoverageAmount(ci.requestedClaimAmount) }}</span>
                    <div class="text-xs text-blue-600 font-medium">Customer's Request</div>
                  } @else {
                    <span class="text-gray-400">—</span>
                  }
                </td>
                <td class="px-6 py-4 text-sm text-gray-700">
                  @if (ci.policyName && ci.maxCoverage != null) {
                    <div class="font-semibold text-gray-900">{{ ci.policyName }}</div>
                    <div class="text-xs text-green-700 font-medium">Max: ₹{{ getPolicyCoverageAmount(ci.maxCoverage) }}</div>
                  } @else {
                    <span class="text-gray-400">—</span>
                  }
                </td>
                <td class="px-6 py-4 text-sm">
                  @if ((ci.customerDocuments?.length || 0) > 0) {
                    <div class="flex flex-col gap-1">
                      @for (doc of ci.customerDocuments!.slice(0, 2); track doc.documentId) {
                        <button type="button" (click)="downloadCustomerDocument(doc)" class="text-left text-xs text-blue-700 hover:text-blue-900 hover:underline">
                          {{ doc.fileName }}
                        </button>
                      }
                      @if ((ci.customerDocuments?.length || 0) > 2) {
                        <span class="text-xs text-gray-500">+{{ (ci.customerDocuments?.length || 0) - 2 }} more</span>
                      }
                    </div>
                  } @else {
                    <span class="text-xs text-gray-400">No customer docs</span>
                  }
                </td>
                <td class="px-6 py-4 text-sm text-gray-600">{{ ci.inspectionDate ? (ci.inspectionDate | date:'mediumDate') : '—' }}</td>
                <td class="px-6 py-4">
                  @if (ci.estimatedLoss != null) {
                    <span class="text-sm font-bold text-gray-900"><span>₹</span>{{ ci.estimatedLoss.toLocaleString() }}</span>
                  } @else { <span class="text-gray-400 text-sm">—</span> }
                </td>
                <td class="px-6 py-4">
                  <span class="px-2.5 py-1 text-xs font-semibold rounded-full" [ngClass]="getStatusClass(ci.status)">
                    {{ ci.status.replace('_', ' ') }}
                  </span>
                </td>
                <td class="px-6 py-4">
                  @if (canSubmitReport(ci.status)) {
                    <button (click)="openReportModal(ci)"
                      class="flex items-center gap-1.5 px-4 py-2 bg-amber-500 text-white text-sm rounded-lg hover:bg-amber-600 transition-colors font-medium">
                      <span class="material-icons text-sm">rate_review</span>Verify Claim
                    </button>
                  } @else { <span class="text-xs text-gray-400 italic">Report submitted</span> }
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
    <div class="bg-white rounded-xl shadow-2xl w-full max-w-2xl" (click)="$event.stopPropagation()">
      <div class="bg-gradient-to-r from-orange-600 to-red-700 px-6 py-4 rounded-t-xl">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="p-2 bg-white/20 rounded-lg"><span class="material-icons text-white">local_fire_department</span></div>
            <div>
              <h2 class="text-white font-bold text-lg">Claim Verification Report</h2>
              <p class="text-white/80 text-sm">#CI-{{ selectedInspection()!.inspectionId }} | Claim #CLM-{{ selectedInspection()!.claimId }}</p>
            </div>
          </div>
          <button (click)="closeReportModal()" class="text-white/70 hover:text-white"><span class="material-icons">close</span></button>
        </div>
      </div>

      <div class="px-6 py-4 overflow-y-auto max-h-[75vh]">
        <!-- Policy & Claim Context Information -->
        <div class="mb-5 p-4 bg-orange-50 border border-orange-200 rounded-lg">
          <h3 class="text-sm font-semibold text-gray-700 mb-3 flex items-center justify-between">
            <span>Policy & Claim Context</span>
            <span class="px-2 py-0.5 bg-orange-100 text-orange-700 rounded-sm text-[10px] uppercase font-bold tracking-wider border border-orange-200">
              #CLM-{{ selectedInspection()!.claimId }}
            </span>
          </h3>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-3 text-sm">
            <div>
              <div class="text-gray-500 text-[10px] font-bold uppercase tracking-wider mb-0.5">Customer Information</div>
              <div class="font-bold text-gray-900">{{ selectedInspection()!.customerName || 'N/A' }}</div>
              <div class="text-xs text-gray-600 flex items-center gap-1"><span class="material-icons text-[10px]">email</span> {{ selectedInspection()!.customerEmail || 'No email' }}</div>
            </div>
            @if (selectedInspection()!.requestedClaimAmount != null) {
              <div>
                <div class="text-gray-500 text-[10px] font-bold uppercase tracking-wider mb-0.5">Customer's Claim Request</div>
                <div class="font-bold text-blue-700">₹{{ getPolicyCoverageAmount(selectedInspection()!.requestedClaimAmount!) }}</div>
                <div class="text-[10px] text-blue-600 font-medium italic">Amount Requested by Customer</div>
              </div>
            }
            <div>
              <div class="text-gray-500 text-[10px] font-bold uppercase tracking-wider mb-0.5">Policy & Maximum Coverage</div>
              @if (selectedInspection()!.policyName && selectedInspection()!.maxCoverage != null) {
                <div class="font-bold text-gray-900">{{ selectedInspection()!.policyName }}</div>
                <div class="text-[10px] text-green-700 font-bold">Limit: ₹{{ getPolicyCoverageAmount(selectedInspection()!.maxCoverage!) }}</div>
              } @else {
                <div class="text-gray-400 italic text-xs">Policy details unavailable</div>
              }
            </div>
            @if (selectedInspection()!.claimDescription) {
              <div class="col-span-1 md:col-span-2 mt-1">
                <div class="text-gray-500 text-[10px] font-bold uppercase tracking-wider mb-1">Claim Description</div>
                <div class="text-gray-700 text-xs bg-white p-2 border border-orange-100 rounded italic">{{ selectedInspection()!.claimDescription }}</div>
              </div>
            }
          </div>
        </div>

        <form [formGroup]="reportForm" class="space-y-6">
          <!-- Cause of Loss -->
          <div class="p-4 bg-gray-50 border border-gray-200 rounded-lg">
            <h3 class="text-sm font-bold text-gray-700 mb-4 border-b border-gray-200 pb-2 flex items-center gap-2">
              <span class="material-icons text-fire-red text-sm">warning</span> Cause Verification
            </h3>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label class="block text-xs font-bold text-gray-600 uppercase tracking-wider mb-1.5">Primary Cause of Fire <span class="text-red-500">*</span></label>
                <select formControlName="causeOfFire"
                  class="w-full px-3 py-2 bg-white border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-fire-red transition-all font-medium text-sm">
                  <option [value]="''">Select Cause</option>
                  <option value="SHORT_CIRCUIT">Electrical Short Circuit</option>
                  <option value="LP_GAS_EXPLOSION">LP Gas Leak/Explosion</option>
                  <option value="KITCHEN_FIRE">Kitchen/Cooking Fire</option>
                  <option value="NATURAL_CALAMITY">Natural Calamity (Lightning/Wildfire)</option>
                  <option value="ARSON">Arson/Malicious Damage</option>
                  <option value="EXTERNAL_SPREAD">Spread from External Property</option>
                  <option value="OTHER">Other Causes</option>
                </select>
              </div>
              <div>
                <label class="block text-xs font-bold text-gray-600 uppercase tracking-wider mb-1.5">Under-Insurance Detected?</label>
                <div class="flex items-center mt-2">
                  <input type="checkbox" formControlName="underInsuranceDetected" id="underInsurance"
                    class="h-4 w-4 text-fire-red border-gray-300 rounded focus:ring-fire-red" />
                  <label for="underInsurance" class="ml-2 text-xs font-medium text-gray-700 leading-tight">
                    Yes (Property undervalued at inception)
                  </label>
                </div>
              </div>
            </div>
          </div>

          <!-- Loss Breakdown -->
          <div class="p-4 bg-gray-50 border border-gray-200 rounded-lg">
            <h3 class="text-sm font-bold text-gray-700 mb-4 border-b border-gray-200 pb-2 flex items-center gap-2">
              <span class="material-icons text-blue-600 text-sm">calculate</span> Loss Breakdown & Financials
            </h3>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label class="block text-xs font-bold text-gray-600 uppercase tracking-wider mb-1.5">Building Loss</label>
                <div class="relative">
                  <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 font-bold text-xs">₹</span>
                  <input type="number" formControlName="buildingLoss" (input)="onLossChange()"
                    class="w-full pl-6 pr-3 py-2 bg-white border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-fire-red text-sm" />
                </div>
              </div>
              <div>
                <label class="block text-xs font-bold text-gray-600 uppercase tracking-wider mb-1.5">Contents Loss</label>
                <div class="relative">
                  <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 font-bold text-xs">₹</span>
                  <input type="number" formControlName="contentsLoss" (input)="onLossChange()"
                    class="w-full pl-6 pr-3 py-2 bg-white border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-fire-red text-sm" />
                </div>
              </div>
              <div class="bg-gray-100 p-2 rounded-lg border border-dashed border-gray-300">
                <label class="block text-[10px] font-bold text-gray-500 uppercase tracking-wider mb-1">Total Estimated Loss</label>
                <div class="text-lg font-black text-gray-900">₹{{ reportForm.get('estimatedLoss')?.value | number:'1.0-0' }}</div>
              </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mt-6">
              <div>
                <label class="block text-xs font-bold text-gray-600 uppercase tracking-wider mb-1.5">Salvage Value <span class="text-xs font-normal text-gray-400">(Deduction)</span></label>
                <div class="relative">
                  <span class="absolute left-3 top-1/2 -translate-y-1/2 text-red-400 font-bold text-xs">-₹</span>
                  <input type="number" formControlName="salvageValue" (input)="onLossChange()"
                    class="w-full pl-8 pr-3 py-2 bg-white border border-gray-100 rounded-lg focus:outline-none focus:ring-2 focus:ring-fire-red text-sm" placeholder="Residual value of remains" />
                </div>
              </div>
              <div>
                <label class="block text-xs font-bold text-gray-600 uppercase tracking-wider mb-1.5">Fire Brigade Expenses</label>
                <div class="relative">
                  <span class="absolute left-3 top-1/2 -translate-y-1/2 text-green-500 font-bold text-xs">+₹</span>
                  <input type="number" formControlName="fireBrigadeExpenses" (input)="onLossChange()"
                    class="w-full pl-8 pr-3 py-2 bg-white border border-gray-100 rounded-lg focus:outline-none focus:ring-2 focus:ring-fire-red text-sm" placeholder="Claimed by authority" />
                </div>
              </div>
            </div>

            <!-- Professional Recommendation -->
            <div class="mt-6 p-4 bg-fire-red/5 border border-fire-red/20 rounded-xl">
              <div class="flex items-center justify-between">
                <div>
                  <h4 class="text-xs font-black text-fire-red-700 uppercase tracking-widest">Recommended Settlement</h4>
                  <p class="text-[10px] text-fire-red-600 italic">Net assessment based on surveyed loss and salvage</p>
                </div>
                <div class="text-2xl font-black text-fire-red">₹{{ reportForm.get('recommendedSettlement')?.value | number:'1.0-0' }}</div>
              </div>
            </div>
          </div>

          <!-- Documentation & Report -->
          <div>
            <label class="block text-xs font-bold text-gray-600 uppercase tracking-wider mb-2">Detailed Investigation Report <span class="text-red-500 font-bold">*</span></label>
            <textarea formControlName="damageReport" rows="5"
              class="w-full px-4 py-3 bg-gray-50 border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-fire-red focus:bg-white transition-all resize-none text-sm placeholder:italic"
              placeholder="Provide a comprehensive technical report including fire origin, policy compliance, and professional opinion..."></textarea>
            @if (reportForm.get('damageReport')?.touched && reportForm.get('damageReport')?.invalid) {
              <p class="text-red-500 text-xs mt-1.5 font-medium animate-pulse">Professional report must be at least 30 characters.</p>
            }
          </div>

          @if (errorMessage()) {
            <div class="flex gap-2 bg-red-50 border border-red-200 text-red-700 p-3 rounded-lg text-sm font-medium">
              <span class="material-icons text-sm">report_problem</span>{{ errorMessage() }}
            </div>
          }
          
          <div class="mt-4 pt-4 border-t border-gray-100">
            <h3 class="text-xs font-bold text-gray-500 uppercase tracking-widest mb-4">Inspection Documentation</h3>
            <app-inspection-document-upload
              [inspectionId]="selectedInspection()!.inspectionId"
              [isClaimInspection]="true"
            ></app-inspection-document-upload>
          </div>
        </form>
      </div>

      <div class="px-8 py-5 border-t border-gray-100 flex gap-4 justify-end bg-gray-50/80 rounded-b-xl shadow-inner-lg">
        <button type="button" (click)="closeReportModal()" class="px-6 py-2.5 text-gray-700 font-bold bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition-all shadow-sm">Cancel</button>
        <button type="button" (click)="submitReport()" [disabled]="isSubmitting() || reportForm.invalid"
          class="flex items-center gap-2 px-8 py-2.5 bg-fire-red text-white font-bold rounded-lg hover:bg-fire-red-700 hover:-translate-y-0.5 transition-all shadow-md disabled:opacity-50 disabled:cursor-not-allowed">
          @if (isSubmitting()) { <span class="material-icons animate-spin text-sm">sync</span> } @else { <span class="material-icons text-sm">verified</span> }
          {{ isSubmitting() ? 'Submitting Report...' : 'Finalize Verification' }}
        </button>
      </div>
    </div>
  </div>
}
  `
})
export class ClaimInspectionsComponent implements OnInit {
  private surveyorService = inject(SurveyorService);
  private documentService = inject(DocumentService);
  private fb = inject(FormBuilder);

  inspections = signal<ClaimInspectionItem[]>([]);
  filteredInspections = signal<ClaimInspectionItem[]>([]);
  isLoading = signal<boolean>(true);
  isSubmitting = signal<boolean>(false);
  errorMessage = signal<string>('');
  successMessage = signal<string>('');
  selectedInspection = signal<ClaimInspectionItem | null>(null);
  showReportModal = signal<boolean>(false);
  activeFilter = signal<string>('ALL');

  reportForm: FormGroup = this.fb.group({
    estimatedLoss: [0, [Validators.required, Validators.min(0), Validators.max(100000000), CustomValidators.positiveNumber()]],
    damageReport: ['', [Validators.required, Validators.minLength(30), Validators.maxLength(5000), CustomValidators.noWhitespace()]],
    // Breakdown fields (UI only for calculation)
    buildingLoss: [0, [Validators.min(0)]],
    contentsLoss: [0, [Validators.min(0)]],
    // Professional fields (Mapped to backend)
    causeOfFire: ['', [Validators.required]],
    salvageValue: [0, [Validators.min(0)]],
    fireBrigadeExpenses: [0, [Validators.min(0)]],
    underInsuranceDetected: [false],
    recommendedSettlement: [0, [Validators.required, Validators.min(0)]]
  });

  onLossChange(): void {
    const building = this.reportForm.get('buildingLoss')?.value || 0;
    const contents = this.reportForm.get('contentsLoss')?.value || 0;
    const salvage = this.reportForm.get('salvageValue')?.value || 0;
    const expenses = this.reportForm.get('fireBrigadeExpenses')?.value || 0;

    const totalLoss = building + contents;
    const netSettlement = Math.max(0, (totalLoss - salvage) + expenses);

    this.reportForm.patchValue({
      estimatedLoss: totalLoss,
      recommendedSettlement: netSettlement
    }, { emitEvent: false });
  }

  ngOnInit(): void { this.loadInspections(); }

  loadInspections(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.surveyorService.getMyClaimInspections().subscribe({
      next: (data) => { this.inspections.set(data); this.applyFilter(this.activeFilter()); this.isLoading.set(false); },
      error: (err) => { console.error(err); this.errorMessage.set('Failed to load claim inspections.'); this.isLoading.set(false); }
    });
  }

  applyFilter(filter: string): void {
    this.activeFilter.set(filter);
    this.filteredInspections.set(filter === 'ALL' ? this.inspections() : this.inspections().filter(i => i.status === filter));
  }

  openReportModal(inspection: ClaimInspectionItem): void {
    this.selectedInspection.set(inspection);
    this.reportForm.reset({
      estimatedLoss: 0,
      buildingLoss: 0,
      contentsLoss: 0,
      damageReport: '',
      causeOfFire: '',
      salvageValue: 0,
      fireBrigadeExpenses: 0,
      underInsuranceDetected: false,
      recommendedSettlement: 0
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
    this.surveyorService.submitClaimInspectionReport(insp.inspectionId, this.reportForm.value).subscribe({
      next: (updated) => {
        this.inspections.set(this.inspections().map(i => i.inspectionId === updated.inspectionId ? updated : i));
        this.applyFilter(this.activeFilter());
        this.isSubmitting.set(false);
        this.closeReportModal();
        this.successMessage.set('Claim inspection report submitted!');
        setTimeout(() => this.successMessage.set(''), 4000);
      },
      error: (err) => { console.error(err); this.isSubmitting.set(false); this.errorMessage.set('Failed to submit report.'); }
    });
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = { 'ASSIGNED': 'bg-blue-100 text-blue-800', 'UNDER_REVIEW': 'bg-yellow-100 text-yellow-800', 'APPROVED': 'bg-green-100 text-green-800', 'REJECTED': 'bg-red-100 text-red-800' };
    return map[status] || 'bg-gray-100 text-gray-800';
  }

  canSubmitReport(status: string): boolean { return status === 'ASSIGNED'; }

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

  /**
   * Formats the policy coverage amount with proper currency formatting
   * for display in the surveyor interface.
   */
  getPolicyCoverageAmount(amount: number | null | undefined): string {
    if (!amount) return '—';

    // Format in Indian currency style with proper separators
    return amount.toLocaleString('en-IN', {
      maximumFractionDigits: 0,
      minimumFractionDigits: 0
    });
  }
}
