
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AdminService, Underwriter, SiuInvestigator, FraudAnalysisResponse } from '../../../../features/admin/services/admin.service';
import { Claim } from '../../../../core/models/claim.model';
import { DocumentService } from '../../../../core/services/document.service';
import { Document } from '../../../../core/models/document.model';

@Component({
  selector: 'app-claims',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './claims.html'
})
export class ClaimsComponent implements OnInit {
  public invalidAssignmentMessage: Record<number, string> = {};

  public canAssignUnderwriter(claim: Claim): boolean {
    // Allow for LOW/MEDIUM risk, or HIGH risk after SIU cleared
    if ((claim.riskLevel === 'LOW' || claim.riskLevel === 'MEDIUM') && !claim.underwriterId) {
      return true;
    }
    // Allow underwriter assignment after SIU clears a HIGH risk claim
    if (claim.riskLevel === 'HIGH' && claim.siuStatus === 'CLEARED' && !claim.underwriterId && claim.status === 'SIU_CLEARED') {
      return true;
    }
    return false;
  }

  public canAssignSiuUI(claim: Claim): boolean {
    // Only allow SIU assignment for HIGH risk, not already assigned, not resolved
    if (claim.riskLevel !== 'HIGH') return false;
    if (this.isSiuAssigned(claim)) return false;
    if ([ 'APPROVED', 'REJECTED', 'SETTLED', 'PAID' ].includes(claim.status)) return false;
    return ['SUBMITTED', 'UNDER_REVIEW', 'INSPECTING', 'INSPECTED'].includes(claim.status);
  }

  public setInvalidAssignmentMessage(claim: Claim, type: 'UW' | 'SIU'): void {
    this.invalidAssignmentMessage[claim.claimId] = 'Invalid assignment for this risk level';
    setTimeout(() => {
      delete this.invalidAssignmentMessage[claim.claimId];
    }, 3000);
  }

  // ...existing code above...
  // (No duplicate properties or methods below this line)
  private adminService = inject(AdminService);
  private router = inject(Router);

  claims = signal<Claim[]>([]);
  filteredClaims = signal<Claim[]>([]);
  isLoading = signal<boolean>(true);
  errorMessage = signal<string>('');
  searchTerm = signal<string>('');

  underwriters = signal<Underwriter[]>([]);
  selectedUnderwriters: Record<number, number> = {};

  siuInvestigators = signal<SiuInvestigator[]>([]);
  selectedSiuInvestigators: Record<number, number> = {};
  
  // Document Viewer logic
  private documentService = inject(DocumentService);
  selectedClaimForDocs = signal<Claim | null>(null);
  claimDocuments = signal<Document[]>([]);
  isDocsLoading = signal(false);
  successMessage = signal('');

  // Fraud Analysis Modal logic
  selectedClaimForAnalysis = signal<Claim | null>(null);
  fraudAnalysis = signal<FraudAnalysisResponse | null>(null);
  isAnalysisLoading = signal<boolean>(false);
  analysisErrorMessage = signal<string>('');
  showFraudAnalysisModal = signal<boolean>(false);

  // Surveyor Report State
  showSurveyReportModal = signal<boolean>(false);
  selectedClaimForSurvey = signal<Claim | null>(null);
  surveyReport = signal<any | null>(null);
  isSurveyLoading = signal<boolean>(false);
  surveyErrorMessage = signal<string>('');

  ngOnInit(): void {
    this.loadClaims();
    this.loadUnderwriters();
    this.loadSiuInvestigators();
  }

  loadUnderwriters(): void {
    this.adminService.getAllUnderwriters().subscribe({
      next: (data) => this.underwriters.set(data || []),
      error: (err) => console.error('Error loading underwriters', err)
    });
  }

  loadSiuInvestigators(): void {
    this.adminService.getAllSiuInvestigators().subscribe({
      next: (data) => {
        this.siuInvestigators.set(data || []);
        console.log(`Loaded ${data?.length || 0} SIU investigators`);
      },
      error: (err) => {
        console.error('Error loading SIU investigators:', err);
        this.errorMessage.set('Failed to load SIU investigators. Some features may not work.');
        setTimeout(() => this.errorMessage.set(''), 4000);
      }
    });
  }

  loadClaims(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.adminService.getAllClaims().subscribe({
      next: (data) => {
        // Apply smart calculation for settlement amounts
        const processedClaims = data.map(claim => this.calculateSettlementAmount(claim));
        this.claims.set(processedClaims);
        this.filteredClaims.set(processedClaims);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading claims:', error);
        this.errorMessage.set('Failed to load claims');
        this.isLoading.set(false);
      }
    });
  }

  searchClaims(term: string): void {
    this.searchTerm.set(term);
    const lowerTerm = term.toLowerCase();
    
    if (!lowerTerm) {
      this.filteredClaims.set(this.claims());
      return;
    }

    const filtered = this.claims().filter(claim =>
      claim.claimId.toString().includes(lowerTerm) ||
      claim.status.toLowerCase().includes(lowerTerm) ||
      claim.description.toLowerCase().includes(lowerTerm)
    );

    this.filteredClaims.set(filtered);
  }

  updateClaimStatus(id: number, status: string): void {
    this.adminService.updateClaimStatus(id, status).subscribe({
      next: () => {
        const updated = this.claims().map(c => 
          c.claimId === id ? { ...c, status: status as any } : c
        );
        this.claims.set(updated);
        this.filteredClaims.set(updated);
      },
      error: (error) => {
        console.error('Error updating claim:', error);
        alert('Failed to update claim status');
      }
    });
  }

  approveClaim(id: number): void {
    const claim = this.claims().find(c => c.claimId === id);

    // Check if claim is under SIU investigation
    if (claim?.siuStatus === 'UNDER_INVESTIGATION') {
      this.errorMessage.set('Cannot approve claim: Currently under SIU investigation');
      setTimeout(() => this.errorMessage.set(''), 5000);
      return;
    }

    if (!confirm('Are you sure you want to approve this claim?')) {
      return;
    }

    this.adminService.approveClaim(id).subscribe({
      next: (updatedClaim) => {
        const updated = this.claims().map(c =>
          c.claimId === id ? updatedClaim : c
        );
        this.claims.set(updated);
        this.filteredClaims.set(updated);
        this.successMessage.set('Claim approved successfully!');
        setTimeout(() => this.successMessage.set(''), 3500);
      },
      error: (error) => {
        console.error('Error approving claim:', error);
        const errorMsg = error.error?.message || 'Failed to approve claim. Please try again.';
        this.errorMessage.set(errorMsg);
        setTimeout(() => this.errorMessage.set(''), 5000);
      }
    });
  }

  rejectClaim(id: number): void {
    const claim = this.claims().find(c => c.claimId === id);

    // Check if claim is under SIU investigation
    if (claim?.siuStatus === 'UNDER_INVESTIGATION') {
      this.errorMessage.set('Cannot reject claim: Currently under SIU investigation');
      setTimeout(() => this.errorMessage.set(''), 5000);
      return;
    }

    if (!confirm('Are you sure you want to reject this claim?')) {
      return;
    }

    this.adminService.rejectClaim(id).subscribe({
      next: (updatedClaim) => {
        const updated = this.claims().map(c =>
          c.claimId === id ? updatedClaim : c
        );
        this.claims.set(updated);
        this.filteredClaims.set(updated);
        this.successMessage.set('Claim rejected successfully!');
        setTimeout(() => this.successMessage.set(''), 3500);
      },
      error: (error) => {
        console.error('Error rejecting claim:', error);
        const errorMsg = error.error?.message || 'Failed to reject claim. Please try again.';
        this.errorMessage.set(errorMsg);
        setTimeout(() => this.errorMessage.set(''), 5000);
      }
    });
  }

  /**
   * Check if claim can be approved/rejected (not under SIU and not already finalized)
   */
  canApproveReject(claim: Claim): boolean {
    return claim.siuStatus !== 'UNDER_INVESTIGATION' &&
           !['APPROVED', 'REJECTED', 'SETTLED'].includes(claim.status);
  }

  /**
   * Check if claim is blocked by SIU
   */
  isClaimBlocked(claim: Claim): boolean {
    return claim.siuStatus === 'UNDER_INVESTIGATION';
  }

  assignUnderwriter(claim: Claim): void {
    const claimId = claim.claimId;
    const underwriterId = this.selectedUnderwriters[claimId];
    // Prevent invalid assignment
    if (claim.underwriterId || !(claim.riskLevel === 'LOW' || claim.riskLevel === 'MEDIUM')) {
      this.setInvalidAssignmentMessage(claim, 'UW');
      return;
    }
    if (!underwriterId) {
      this.successMessage.set('');
      alert('Please select an underwriter first');
      return;
    }
    this.adminService.assignUnderwriterToClaim(claimId, underwriterId).subscribe({
      next: () => {
        this.successMessage.set('Underwriter assigned to claim successfully!');
        // Update local state to hide the assign button immediately
        const updatedClaims = this.claims().map(c => {
          if (c.claimId === claimId) {
            return { ...c, underwriterId: underwriterId };
          }
          return c;
        });
        this.claims.set(updatedClaims);
        this.filteredClaims.set(updatedClaims);
        setTimeout(() => this.successMessage.set(''), 3500);
      },
      error: (err) => {
        console.error('Error assigning underwriter to claim:', err);
        // Handle both object-based and text-based error responses
        let msg = 'Failed to assign underwriter.';
        if (typeof err.error === 'string') {
          msg = err.error;
        } else if (err.error?.message) {
          msg = err.error.message;
        }
        alert(msg);
      }
    });
  }

  assignSiuToClaim(claim: Claim): void {
    const claimId = claim.claimId;
    const investigatorId = this.selectedSiuInvestigators[claimId];
    // Prevent invalid assignment
    if (this.isSiuAssigned(claim) || claim.riskLevel !== 'HIGH') {
      this.setInvalidAssignmentMessage(claim, 'SIU');
      return;
    }
    if (!investigatorId) {
      this.errorMessage.set('Please select an SIU investigator first');
      setTimeout(() => this.errorMessage.set(''), 4000);
      return;
    }
    // Get investigator name for confirmation
    const investigator = this.siuInvestigators().find(inv => inv.investigatorId === investigatorId);
    const investigatorName = investigator?.username || 'Unknown Investigator';
    // Confirmation dialog
    if (!confirm(`Are you sure you want to assign ${investigatorName} to investigate Claim #CLM-${claimId}? This will mark the claim as under SIU investigation and block approval/rejection until investigation is complete.`)) {
      return;
    }
    this.adminService.assignSiuToClaim(claimId, investigatorId).subscribe({
      next: () => {
        this.successMessage.set(`SIU investigator ${investigatorName} assigned to claim #CLM-${claimId} successfully!`);
        // Update local state to reflect SIU assignment
        const updatedClaims = this.claims().map(c => {
          if (c.claimId === claimId) {
            return { ...c, siuStatus: 'UNDER_INVESTIGATION' };
          }
          return c;
        });
        this.claims.set(updatedClaims);
        this.filteredClaims.set(updatedClaims);
        // Clear selection
        delete this.selectedSiuInvestigators[claimId];
        setTimeout(() => this.successMessage.set(''), 5000);
      },
      error: (err) => {
        console.error('Error assigning SIU investigator to claim:', err);
        const msg = err.error?.message || 'Failed to assign SIU investigator. Please try again.';
        this.errorMessage.set(msg);
        setTimeout(() => this.errorMessage.set(''), 5000);
      }
    });
  }

  /**
   * Check if claim can be assigned to SIU (not already under investigation and not finalized)
   */
  canAssignSiu(claim: Claim): boolean {
    // Don't allow SIU assignment if already under investigation
    if (claim.siuStatus === 'UNDER_INVESTIGATION') {
      return false;
    }

    // Don't allow SIU assignment if claim is already finalized
    if (['APPROVED', 'REJECTED', 'SETTLED', 'PAID'].includes(claim.status)) {
      return false;
    }

    // Only allow SIU assignment for claims that have been submitted and potentially inspected
    return ['SUBMITTED', 'UNDER_REVIEW', 'INSPECTING', 'INSPECTED'].includes(claim.status);
  }

  /**
   * Check if claim is currently assigned to an SIU investigator
   */
  isSiuAssigned(claim: Claim): boolean {
    return claim.siuStatus === 'UNDER_INVESTIGATION' ||
           claim.siuStatus === 'FRAUD_CONFIRMED' ||
           claim.siuStatus === 'CLEARED';
  }

  viewDocuments(claim: Claim): void {
    this.selectedClaimForDocs.set(claim);
    this.isDocsLoading.set(true);
    // Fetch specifically CLAIM docs by claimId
    this.documentService.getDocumentsForEntity(claim.claimId, 'CLAIM').subscribe({
      next: (docs) => {
        // filter to claim stage docs
        const claimLevelDocs = docs.filter((d: any) =>
          d.documentStage === 'CLAIM_STAGE' ||
          d.documentType === 'CLAIM_FORM' ||
          d.documentType === 'SPOT_SURVEY_REPORT' ||
          d.documentType === 'FIRE_BRIGADE_REPORT' ||
          d.documentType === 'DAMAGE_PHOTOS' ||
          d.documentType === 'REPAIR_ESTIMATE' ||
          d.documentType === 'OTHER'
        );
        this.claimDocuments.set(claimLevelDocs);
        this.isDocsLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load claim documents:', err);
        this.isDocsLoading.set(false);
      }
    });
  }

  viewAnalysis(claim: Claim): void {
    this.selectedClaimForAnalysis.set(claim);
    this.isAnalysisLoading.set(true);
    this.analysisErrorMessage.set('');
    this.showFraudAnalysisModal.set(true);

    this.adminService.getFraudAnalysis(claim.claimId).subscribe({
      next: (analysis) => {
        this.fraudAnalysis.set(analysis);
        this.isAnalysisLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading fraud analysis:', error);
        this.analysisErrorMessage.set('Failed to load fraud analysis. Please try again.');
        this.isAnalysisLoading.set(false);
      }
    });
  }

  viewSurveyReport(claim: Claim): void {
    this.selectedClaimForSurvey.set(claim);
    this.isSurveyLoading.set(true);
    this.surveyErrorMessage.set('');
    this.showSurveyReportModal.set(true);

    this.adminService.getClaimInspectionByClaimId(claim.claimId).subscribe({
      next: (report) => {
        this.surveyReport.set(report);
        this.isSurveyLoading.set(false);
      },
      error: (err) => {
        console.error('Error loading survey report:', err);
        this.surveyErrorMessage.set('Survey report not found or system error. Ensure the surveyor has submitted the report.');
        this.isSurveyLoading.set(false);
      }
    });
  }

  closeSurveyModal(): void {
    this.showSurveyReportModal.set(false);
    this.selectedClaimForSurvey.set(null);
    this.surveyReport.set(null);
    this.surveyErrorMessage.set('');
  }

  closeDocsModal(): void {
    this.selectedClaimForDocs.set(null);
    this.claimDocuments.set([]);
    this.isDocsLoading.set(false);
  }

  closeFraudAnalysisModal(): void {
    this.showFraudAnalysisModal.set(false);
    this.selectedClaimForAnalysis.set(null);
    this.fraudAnalysis.set(null);
    this.analysisErrorMessage.set('');
  }

  downloadDocument(doc: Document): void {
    this.documentService.downloadDocument(doc.documentId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = doc.fileName || 'Document';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: (err) => console.error('Download failed', err)
    });
  }

  /**
   * Refresh SIU investigators list
   */
  refreshSiuInvestigators(): void {
    console.log('Refreshing SIU investigators list...');
    this.loadSiuInvestigators();
  }

  /**
   * Get assigned SIU investigator name for display
   */
  getAssignedSiuInvestigator(claim: Claim): string {
    // This would typically come from the claim data if it includes investigator info
    // For now, return a generic message
    return 'SIU Investigator';
  }
  private calculateSettlementAmount(claim: Claim): Claim {
    const estimatedLoss = Number(claim.estimatedLoss) || 0;
    const deductible = Number(claim.deductible) || 0;
    const depreciation = Number(claim.depreciation) || 0;
    const backendSettlement = Number(claim.settlementAmount) || 0;

    // Consistency check: The settlement amount must match its breakdown
    if (estimatedLoss > 0) {
      const calculatedAmount = Math.max(0, estimatedLoss - deductible - depreciation);
      
      // Override or fix the settlement amount to match its components
      return { ...claim, settlementAmount: calculatedAmount };
    }

    return claim;
  }
}
