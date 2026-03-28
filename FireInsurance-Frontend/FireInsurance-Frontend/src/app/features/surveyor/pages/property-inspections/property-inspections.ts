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
        [class]="activeFilter() === filter ? 'bg-[#C72B32] text-white shadow-sm' : 'text-gray-600 hover:bg-gray-100'">
        {{ filter }}
      </button>
    }
  </div>

  @if (isLoading()) {
    <div class="flex items-center justify-center py-16">
      <div class="w-10 h-10 border-4 border-gray-200 border-t-[#C72B32] rounded-full animate-spin"></div>
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
              <th class="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">Requested Coverage</th>
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
                <td colspan="10" class="px-6 py-16 text-center">
                  <span class="material-icons text-5xl text-gray-300 mb-3 block">home_work</span>
                  <p class="text-gray-500 font-medium">No inspections found</p>
                  <p class="text-gray-400 text-sm mt-1">Check back when the admin assigns inspections to you</p>
                </td>
              </tr>
            }
            @for (insp of filteredInspections(); track insp.inspectionId) {
              <tr class="hover:bg-gray-50 transition-colors" [class.bg-orange-50]="insp.isDuplicateProperty" [class.border-l-4]="insp.isDuplicateProperty" [class.border-orange-400]="insp.isDuplicateProperty">
                <td class="px-6 py-4">
                  <div class="flex items-center gap-2">
                    <span class="text-sm font-bold text-gray-900">#INS-{{ insp.inspectionId }}</span>
                    @if (insp.isDuplicateProperty) {
                      <span class="inline-flex items-center px-2 py-1 text-xs font-medium bg-orange-100 text-orange-800 rounded-full">
                        <span class="material-icons text-xs mr-1">content_copy</span>
                        Duplicate
                      </span>
                    }
                  </div>
                </td>
                <td class="px-6 py-4 text-sm text-gray-700">
                  <div class="flex items-center gap-2">
                    <span class="material-icons text-blue-500 text-lg">home</span>
                    <div>
                      <div class="font-medium">#PROP-{{ insp.propertyId }}</div>
                      @if (insp.isDuplicateProperty) {
                        <div class="text-xs text-orange-600 font-medium">
                          <span class="material-icons text-xs">info</span>
                          Already inspected (INS-{{ insp.referenceInspectionId }})
                        </div>
                      }
                    </div>
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
                <td class="px-6 py-4 text-sm text-gray-700">
                  @if (insp.requestedSumInsured != null) {
                    <div class="font-semibold text-blue-700">₹{{ getPolicyCoverageAmount(insp.requestedSumInsured) }}</div>
                    <div class="text-xs text-blue-600 font-medium">Requested by Customer</div>
                  } @else {
                    <span class="text-gray-400">—</span>
                  }
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
                  @if (insp.isDuplicateProperty && insp.existingRiskScore != null) {
                    <div class="flex items-center gap-2">
                      <span [class]="getRiskClass(insp.existingRiskScore)">
                        {{ insp.existingRiskScore }}/10
                      </span>
                      <span class="text-xs text-orange-600 font-medium bg-orange-100 px-2 py-1 rounded">
                        From INS-{{ insp.referenceInspectionId }}
                      </span>
                    </div>
                  } @else {
                    <span [class]="getRiskClass(insp.assessedRiskScore)">
                      {{ insp.assessedRiskScore != null ? (insp.assessedRiskScore + '/10') : '—' }}
                    </span>
                  }
                </td>
                <td class="px-6 py-4">
                  <span class="px-2.5 py-1 text-xs font-semibold rounded-full" [ngClass]="getStatusClass(insp.status)">
                    {{ insp.status }}
                  </span>
                </td>
                <td class="px-6 py-4">
                  @if (insp.status === 'ASSIGNED') {
                    @if (insp.isDuplicateProperty) {
                      <button (click)="openReportModal(insp)"
                        class="flex items-center gap-1.5 px-4 py-2 bg-orange-600 text-white text-sm rounded-lg hover:bg-orange-700 transition-colors font-medium">
                        <span class="material-icons text-sm">content_copy</span>
                        Apply Existing Score
                      </button>
                    } @else {
                      <button (click)="openReportModal(insp)"
                        class="flex items-center gap-1.5 px-4 py-2 bg-[#C72B32] text-white text-sm rounded-lg hover:bg-[#A01E28] transition-colors font-medium">
                        <span class="material-icons text-sm">upload_file</span>
                        Submit Report
                      </button>
                    }
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
      <div class="bg-gradient-to-r from-[#C72B32] to-[#A01E28] px-6 py-4 rounded-t-xl" [class.from-orange-600]="selectedInspection()!.isDuplicateProperty" [class.to-orange-700]="selectedInspection()!.isDuplicateProperty">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="p-2 bg-white/20 rounded-lg">
              <span class="material-icons text-white">
                {{ selectedInspection()!.isDuplicateProperty ? 'content_copy' : 'home_work' }}
              </span>
            </div>
            <div>
              @if (selectedInspection()!.isDuplicateProperty) {
                <h2 class="text-white font-bold text-lg">Apply Existing Risk Assessment</h2>
                <p class="text-white/80 text-sm">Property already inspected - applying results from INS-{{ selectedInspection()!.referenceInspectionId }}</p>
              } @else {
                <h2 class="text-white font-bold text-lg">Submit Inspection Report</h2>
                <p class="text-white/80 text-sm">Inspection #INS-{{ selectedInspection()!.inspectionId }}</p>
              }
            </div>
          </div>
          <button (click)="closeReportModal()" class="text-white/70 hover:text-white"><span class="material-icons">close</span></button>
        </div>
      </div>
      <div class="p-6">
        <!-- Duplicate Property Warning -->
        @if (selectedInspection()!.isDuplicateProperty) {
          <div class="mb-5 p-4 bg-orange-50 border border-orange-200 rounded-lg">
            <div class="flex items-start gap-3">
              <span class="material-icons text-orange-600 mt-0.5">info</span>
              <div class="flex-1">
                <h3 class="text-sm font-semibold text-orange-800 mb-1">Property Already Inspected</h3>
                <p class="text-sm text-orange-700 mb-2">
                  This property (#PROP-{{ selectedInspection()!.propertyId }}) was already inspected in inspection
                  #INS-{{ selectedInspection()!.referenceInspectionId }} with a risk score of
                  <span class="font-semibold">{{ selectedInspection()!.existingRiskScore }}/10</span>.
                </p>
                <p class="text-xs text-orange-600">
                  The existing risk assessment will be applied to this policy subscription to avoid duplicate work.
                </p>
              </div>
            </div>
          </div>
        }
        <!-- Policy & Customer Context Information -->
        <div class="mb-5 p-4 bg-blue-50 border border-blue-200 rounded-lg">
          <h3 class="text-sm font-semibold text-gray-700 mb-3">Policy & Customer Context</h3>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
            <div>
              <div class="text-gray-500 text-xs font-medium">Customer Information</div>
              <div class="font-semibold text-gray-900">{{ selectedInspection()!.customerName || 'Customer Name Not Available' }}</div>
              <div class="text-xs text-gray-600">{{ selectedInspection()!.customerEmail || 'No email provided' }}</div>
              <div class="text-xs text-gray-600">{{ selectedInspection()!.customerPhone || 'No phone provided' }}</div>
            </div>
            <div>
              <div class="text-gray-500 text-xs font-medium">Property Details</div>
              <div class="font-medium text-gray-900">{{ selectedInspection()!.propertyType || 'Property type not specified' }}</div>
              <div class="text-xs text-gray-600">{{ selectedInspection()!.propertyAddress || 'Address not provided' }}</div>
            </div>
            <div>
              <div class="text-gray-500 text-xs font-medium">Customer's Requested Coverage</div>
              @if (selectedInspection()!.requestedSumInsured != null) {
                <div class="font-semibold text-blue-700">₹{{ getPolicyCoverageAmount(selectedInspection()!.requestedSumInsured!) }}</div>
                <div class="text-xs text-blue-600 font-medium">Sum Insured Requested</div>
              } @else {
                <div class="text-gray-500 italic">Coverage amount to be determined</div>
              }
            </div>
            <div>
              <div class="text-gray-500 text-xs font-medium">Policy & Maximum Coverage</div>
              @if (selectedInspection()!.policyName && selectedInspection()!.maxCoverage != null) {
                <div class="font-semibold text-gray-900">{{ selectedInspection()!.policyName }}</div>
                <div class="text-xs text-green-700 font-medium">Max Coverage: ₹{{ getPolicyCoverageAmount(selectedInspection()!.maxCoverage!) }}</div>
              } @else {
                <div class="text-gray-500 italic">Policy details being processed</div>
              }
            </div>
          </div>
        </div>

        <form [formGroup]="reportForm" class="space-y-5">
          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-1.5">
              Risk Score <span class="text-red-500">*</span>
              <span class="font-normal text-gray-500">(0 = Low, 10 = High)</span>
              @if (selectedInspection()!.isDuplicateProperty) {
                <span class="ml-2 text-xs bg-orange-100 text-orange-800 px-2 py-1 rounded-full font-medium">
                  Auto-populated from INS-{{ selectedInspection()!.referenceInspectionId }}
                </span>
              }
            </label>
            <input
              type="number"
              formControlName="assessedRiskScore"
              step="0.1"
              min="0"
              max="10"
              [class.bg-orange-50]="selectedInspection()!.isDuplicateProperty"
              [class.border-orange-200]="selectedInspection()!.isDuplicateProperty"
              [class.cursor-not-allowed]="selectedInspection()!.isDuplicateProperty"
              class="w-full px-4 py-3 bg-gray-50 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#C72B32] focus:bg-white transition-colors"
              placeholder="e.g. 3.5" />
            @if (reportForm.get('assessedRiskScore')?.touched && reportForm.get('assessedRiskScore')?.invalid) {
              <p class="text-red-500 text-xs mt-1">Risk score between 0 and 10 is required.</p>
            }
          </div>

          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-1.5">
              Inspection Remarks <span class="text-red-500">*</span>
              @if (selectedInspection()!.isDuplicateProperty) {
                <span class="ml-2 text-xs bg-orange-100 text-orange-800 px-2 py-1 rounded-full font-medium">
                  Auto-generated
                </span>
              }
            </label>
            <textarea
              formControlName="remarks"
              rows="5"
              [class.bg-orange-50]="selectedInspection()!.isDuplicateProperty"
              [class.border-orange-200]="selectedInspection()!.isDuplicateProperty"
              [class.cursor-not-allowed]="selectedInspection()!.isDuplicateProperty"
              class="w-full px-4 py-3 bg-gray-50 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#C72B32] focus:bg-white transition-colors resize-none"
              placeholder="Describe the property condition, fire safety measures observed, and overall assessment..."></textarea>
            @if (reportForm.get('remarks')?.touched && reportForm.get('remarks')?.invalid) {
              <p class="text-red-500 text-xs mt-1">Remarks must be at least 20 characters.</p>
            }
          </div>

          @if (selectedInspection()!.isDuplicateProperty) {
            <div class="p-4 bg-orange-50 border border-orange-200 rounded-lg">
              <h4 class="text-sm font-semibold text-orange-800 mb-2">Risk Assessment Details (from previous inspection):</h4>
              <div class="grid grid-cols-2 gap-3 text-sm">
                <div class="flex items-center gap-2 text-orange-700">
                  <span class="material-icons text-sm">{{ selectedInspection()!.existingRiskData?.fireSafetyAvailable ? 'check_circle' : 'cancel' }}</span>
                  Fire Safety: {{ selectedInspection()!.existingRiskData?.fireSafetyAvailable ? 'Available' : 'Not Available' }}
                </div>
                <div class="flex items-center gap-2 text-orange-700">
                  <span class="material-icons text-sm">{{ selectedInspection()!.existingRiskData?.sprinklerSystem ? 'check_circle' : 'cancel' }}</span>
                  Sprinkler System: {{ selectedInspection()!.existingRiskData?.sprinklerSystem ? 'Available' : 'Not Available' }}
                </div>
                <div class="flex items-center gap-2 text-orange-700">
                  <span class="material-icons text-sm">{{ selectedInspection()!.existingRiskData?.fireExtinguishers ? 'check_circle' : 'cancel' }}</span>
                  Fire Extinguishers: {{ selectedInspection()!.existingRiskData?.fireExtinguishers ? 'Available' : 'Not Available' }}
                </div>
                <div class="text-orange-700">
                  <span class="font-medium">Distance from Fire Station:</span>
                  {{ selectedInspection()!.existingRiskData?.distanceFromFireStation || 'N/A' }} km
                </div>
                @if (selectedInspection()!.existingRiskData?.constructionRisk != null) {
                  <div class="text-orange-700">
                    <span class="font-medium">Construction Risk:</span> {{ selectedInspection()!.existingRiskData?.constructionRisk }}
                  </div>
                }
                @if (selectedInspection()!.existingRiskData?.hazardRisk != null) {
                  <div class="text-orange-700">
                    <span class="font-medium">Hazard Risk:</span> {{ selectedInspection()!.existingRiskData?.hazardRisk }}
                  </div>
                }
              </div>
            </div>
          } @else {
            <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
              <label class="flex items-center gap-2 text-sm text-gray-700"><input type="checkbox" formControlName="fireSafetyAvailable"> Fire safety available</label>
              <label class="flex items-center gap-2 text-sm text-gray-700"><input type="checkbox" formControlName="sprinklerSystem"> Sprinkler system available</label>
              <label class="flex items-center gap-2 text-sm text-gray-700"><input type="checkbox" formControlName="fireExtinguishers"> Fire extinguishers available</label>
              <label class="flex items-center gap-2 text-sm text-gray-700"><input type="checkbox" formControlName="hazardousMaterialsPresent"> Hazardous materials present</label>
            </div>

            <!-- Enhanced COPE Fields -->
            <div class="p-4 bg-gray-50 border border-gray-200 rounded-lg space-y-4">
              <h4 class="text-sm font-bold text-gray-800 border-b border-gray-200 pb-2">COPE Risk Assessment</h4>
              
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label class="block text-xs font-semibold text-gray-600 mb-1">Construction Type</label>
                  <select formControlName="constructionType" class="w-full px-3 py-2 bg-white border border-gray-300 rounded-lg text-sm">
                    <option value="FRAME">Frame (Combustible)</option>
                    <option value="JOISTED_MASONRY">Joisted Masonry</option>
                    <option value="NON_COMBUSTIBLE">Non-Combustible</option>
                    <option value="FIRE_RESISTIVE">Fire Resistive</option>
                  </select>
                </div>
                <div>
                  <label class="block text-xs font-semibold text-gray-600 mb-1">Roof Type</label>
                  <select formControlName="roofType" class="w-full px-3 py-2 bg-white border border-gray-300 rounded-lg text-sm">
                    <option value="ASPHALT">Asphalt Shingles</option>
                    <option value="METAL">Metal</option>
                    <option value="TILE">Concrete/Clay Tile</option>
                    <option value="CONCRETE">Reinforced Concrete</option>
                  </select>
                </div>
                <div>
                  <label class="block text-xs font-semibold text-gray-600 mb-1">Occupancy Tier</label>
                  <select formControlName="occupancyType" class="w-full px-3 py-2 bg-white border border-gray-300 rounded-lg text-sm">
                    <option value="RESIDENTIAL">Residential</option>
                    <option value="COMMERCIAL">Commercial Office/Retail</option>
                    <option value="INDUSTRIAL">Industrial/Manufacturing</option>
                    <option value="WAREHOUSE">Warehouse/Storage</option>
                  </select>
                </div>
                <div>
                  <label class="block text-xs font-semibold text-gray-600 mb-1">Electrical Audit</label>
                  <select formControlName="electricalAuditStatus" class="w-full px-3 py-2 bg-white border border-gray-300 rounded-lg text-sm">
                    <option value="PASS">Pass (Compliant)</option>
                    <option value="FAIL">Fail (Hazards Found)</option>
                    <option value="PENDING">Pending Minor Repairs</option>
                  </select>
                </div>
              </div>

              <div class="grid grid-cols-1 md:grid-cols-3 gap-3">
                <div>
                  <label class="block text-xs font-semibold text-gray-600 mb-1">Dist. to Stn (km)</label>
                  <input type="number" formControlName="distanceFromFireStation" class="w-full px-3 py-2 bg-white border border-gray-300 rounded-lg text-sm" min="0" step="0.1">
                </div>
                <div>
                  <label class="block text-xs font-semibold text-gray-600 mb-1">Adj. Bldg Dist (m)</label>
                  <input type="number" formControlName="adjacentBuildingDistance" class="w-full px-3 py-2 bg-white border border-gray-300 rounded-lg text-sm" min="0" step="1">
                </div>
                <div>
                  <label class="block text-xs font-semibold text-gray-600 mb-1">Hazard Risk (0-1)</label>
                  <input type="number" formControlName="hazardRisk" class="w-full px-3 py-2 bg-white border border-gray-300 rounded-lg text-sm" min="0" max="1" step="0.1">
                </div>
              </div>

              <div>
                <label class="block text-xs font-semibold text-gray-600 mb-1">Internal Protection Notes</label>
                <textarea formControlName="internalProtectionNotes" rows="2" class="w-full px-3 py-2 bg-white border border-gray-300 rounded-lg text-sm resize-none" placeholder="Notes on fire doors, secondary exits, etc..."></textarea>
              </div>
            </div>
          }
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
        @if (selectedInspection()!.isDuplicateProperty) {
          <button type="button" (click)="submitReport()" [disabled]="isSubmitting()"
            class="flex items-center gap-2 px-5 py-2.5 bg-orange-600 text-white font-medium rounded-lg hover:bg-orange-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
            @if (isSubmitting()) { <span class="material-icons animate-spin text-sm">autorenew</span> } @else { <span class="material-icons text-sm">content_copy</span> }
            {{ isSubmitting() ? 'Applying...' : 'Apply Existing Assessment' }}
          </button>
        } @else {
          <button type="button" (click)="submitReport()" [disabled]="isSubmitting() || reportForm.invalid"
            class="flex items-center gap-2 px-5 py-2.5 bg-[#C72B32] text-white font-medium rounded-lg hover:bg-[#A01E28] transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
            @if (isSubmitting()) { <span class="material-icons animate-spin text-sm">autorenew</span> } @else { <span class="material-icons text-sm">check_circle</span> }
            {{ isSubmitting() ? 'Submitting...' : 'Submit New Report' }}
          </button>
        }
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
    constructionType: ['FIRE_RESISTIVE'],
    roofType: ['CONCRETE'],
    occupancyType: ['RESIDENTIAL'],
    electricalAuditStatus: ['PASS'],
    hazardousMaterialsPresent: [false],
    adjacentBuildingDistance: [null, [Validators.min(0)]],
    internalProtectionNotes: ['', [Validators.maxLength(1000)]]
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

    // Auto-populate form with existing data for duplicate properties
    if (inspection.isDuplicateProperty && inspection.existingRiskData) {
      this.reportForm.reset({
        assessedRiskScore: inspection.existingRiskData.assessedRiskScore,
        remarks: `Risk assessment applied from previous inspection #INS-${inspection.referenceInspectionId} of the same property. Original assessment: ${inspection.existingRiskData.assessedRiskScore}/10 risk score.`,
        fireSafetyAvailable: inspection.existingRiskData.fireSafetyAvailable || false,
        sprinklerSystem: inspection.existingRiskData.sprinklerSystem || false,
        fireExtinguishers: inspection.existingRiskData.fireExtinguishers || false,
        distanceFromFireStation: inspection.existingRiskData.distanceFromFireStation || null,
        constructionRisk: inspection.existingRiskData.constructionRisk || null,
        hazardRisk: inspection.existingRiskData.hazardRisk || null,
        constructionType: (inspection as any).existingRiskData?.constructionType || 'FIRE_RESISTIVE',
        roofType: (inspection as any).existingRiskData?.roofType || 'CONCRETE',
        occupancyType: (inspection as any).existingRiskData?.occupancyType || 'RESIDENTIAL',
        electricalAuditStatus: (inspection as any).existingRiskData?.electricalAuditStatus || 'PASS',
        hazardousMaterialsPresent: (inspection as any).existingRiskData?.hazardousMaterialsPresent || false,
        adjacentBuildingDistance: (inspection as any).existingRiskData?.adjacentBuildingDistance || null,
        internalProtectionNotes: (inspection as any).existingRiskData?.internalProtectionNotes || ''
      });

      // Disable form fields for duplicate properties (they can't edit the assessment)
      this.reportForm.get('assessedRiskScore')?.disable();
      this.reportForm.get('fireSafetyAvailable')?.disable();
      this.reportForm.get('sprinklerSystem')?.disable();
      this.reportForm.get('fireExtinguishers')?.disable();
      this.reportForm.get('distanceFromFireStation')?.disable();
      this.reportForm.get('constructionRisk')?.disable();
      this.reportForm.get('hazardRisk')?.disable();
    } else {
      // Normal form reset for new inspections
      this.reportForm.reset({
        assessedRiskScore: null,
        remarks: '',
        fireSafetyAvailable: false,
        sprinklerSystem: false,
        fireExtinguishers: false,
        distanceFromFireStation: null,
        constructionRisk: null,
        hazardRisk: null,
        constructionType: 'FIRE_RESISTIVE',
        roofType: 'CONCRETE',
        occupancyType: 'RESIDENTIAL',
        electricalAuditStatus: 'PASS',
        hazardousMaterialsPresent: false,
        adjacentBuildingDistance: null,
        internalProtectionNotes: ''
      });

      // Ensure fields are enabled for new inspections
      this.reportForm.get('assessedRiskScore')?.enable();
      this.reportForm.get('fireSafetyAvailable')?.enable();
      this.reportForm.get('sprinklerSystem')?.enable();
      this.reportForm.get('fireExtinguishers')?.enable();
      this.reportForm.get('distanceFromFireStation')?.enable();
      this.reportForm.get('constructionRisk')?.enable();
      this.reportForm.get('hazardRisk')?.enable();
    }

    this.showReportModal.set(true);
    this.errorMessage.set('');
  }

  closeReportModal(): void { this.showReportModal.set(false); this.selectedInspection.set(null); this.reportForm.reset(); }

  submitReport(): void {
    const insp = this.selectedInspection();
    if (!insp) return;

    // For duplicate properties, temporarily enable fields for submission
    const wasDuplicate = insp.isDuplicateProperty;
    if (wasDuplicate) {
      // Re-enable all form fields temporarily
      Object.keys(this.reportForm.controls).forEach(key => {
        this.reportForm.get(key)?.enable();
      });
    }

    // Validate form (skip validation for duplicate properties since data is pre-populated)
    if (!wasDuplicate && this.reportForm.invalid) {
      this.reportForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);

    const formData = this.reportForm.value;

    this.surveyorService.submitPropertyInspectionReport(insp.inspectionId, formData).subscribe({
      next: (updated) => {
        this.inspections.set(this.inspections().map(i => i.inspectionId === updated.inspectionId ? updated : i));
        this.applyFilter(this.activeFilter());
        this.isSubmitting.set(false);
        this.closeReportModal();

        if (wasDuplicate) {
          this.successMessage.set('Existing risk assessment applied successfully! No duplicate inspection required.');
        } else {
          this.successMessage.set('Inspection report submitted successfully!');
        }

        setTimeout(() => this.successMessage.set(''), 5000);
      },
      error: (err) => {
        console.error(err);
        this.isSubmitting.set(false);
        this.errorMessage.set('Failed to submit report.');

        // Re-disable fields if it was a duplicate property
        if (wasDuplicate && insp.existingRiskData) {
          this.reportForm.get('assessedRiskScore')?.disable();
          this.reportForm.get('fireSafetyAvailable')?.disable();
          this.reportForm.get('sprinklerSystem')?.disable();
          this.reportForm.get('fireExtinguishers')?.disable();
          this.reportForm.get('distanceFromFireStation')?.disable();
          this.reportForm.get('constructionRisk')?.disable();
          this.reportForm.get('hazardRisk')?.disable();
        }
      }
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

  /**
   * Formats the policy coverage amount with proper currency formatting
   * and handles multiple potential field names for robust mapping.
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
