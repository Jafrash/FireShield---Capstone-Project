import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, Underwriter } from '../../../../features/admin/services/admin.service';
import { Claim, RiskLevel, FraudStatus, FraudAnalysis, SiuAssignmentRequest } from '../../../../core/models/claim.model';
import { Surveyor } from '../../../../features/admin/services/admin.service';
import { DocumentService } from '../../../../core/services/document.service';
import { Document } from '../../../../core/models/document.model';
import { FraudService, SiuInvestigator } from '../../../../core/services/fraud.service';

@Component({
  selector: 'app-claims',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './claims.html'
})
export class ClaimsComponent implements OnInit {
  private adminService = inject(AdminService);
  private fraudService = inject(FraudService);

  claims = signal<Claim[]>([]);
  filteredClaims = signal<Claim[]>([]);
  isLoading = signal<boolean>(true);
  errorMessage = signal<string>('');
  searchTerm = signal<string>('');

  // Risk level filter
  selectedRiskLevel = signal<RiskLevel | 'ALL'>('ALL');

  surveyors = signal<Surveyor[]>([]);
  selectedSurveyors: Record<number, number> = {};

  underwriters = signal<Underwriter[]>([]);
  selectedUnderwriters: Record<number, number> = {};

  // SIU Investigators
  siuInvestigators = signal<SiuInvestigator[]>([]);
  isLoadingInvestigators = signal(false);

  // Document Viewer logic
  private documentService = inject(DocumentService);
  selectedClaimForDocs = signal<Claim | null>(null);
  claimDocuments = signal<Document[]>([]);
  isDocsLoading = signal(false);
  successMessage = signal('');

  // Fraud Detection UI
  selectedClaimForFraud = signal<Claim | null>(null);
  fraudAnalysis = signal<FraudAnalysis | null>(null);
  showFraudModal = signal(false);
  showSiuModal = signal(false);
  isLoadingFraud = signal(false);

  // SIU Assignment
  siuAssignment = signal<SiuAssignmentRequest>({
    siuInvestigatorId: 1,
    initialNotes: ''
  });

  // Computed filtered claims based on risk level
  riskFilteredClaims = computed(() => {
    const claims = this.claims();
    const riskLevel = this.selectedRiskLevel();
    const search = this.searchTerm();

    let filtered = claims;

    // Filter by risk level
    if (riskLevel !== 'ALL') {
      filtered = filtered.filter(claim => claim.riskLevel === riskLevel);
    }

    // Filter by search term
    if (search.trim()) {
      const searchLower = search.toLowerCase().trim();
      filtered = filtered.filter(claim =>
        claim.claimId.toString().includes(searchLower) ||
        claim.description.toLowerCase().includes(searchLower)
      );
    }

    return filtered;
  });

  ngOnInit(): void {
    this.loadClaims();
    this.loadSurveyors();
    this.loadUnderwriters();
    this.loadSiuInvestigators();
  }

  loadSurveyors(): void {
    this.adminService.getAllSurveyors().subscribe({
      next: (data) => this.surveyors.set(data),
      error: (err) => console.error('Error loading surveyors', err)
    });
  }

  loadUnderwriters(): void {
    this.adminService.getAllUnderwriters().subscribe({
      next: (data) => this.underwriters.set(data || []),
      error: (err) => console.error('Error loading underwriters', err)
    });
  }

  loadSiuInvestigators(): void {
    this.isLoadingInvestigators.set(true);
    console.log('Loading SIU investigators...');

    this.fraudService.getSiuInvestigators().subscribe({
      next: (data) => {
        console.log('Loaded SIU investigators:', data);
        this.siuInvestigators.set(data || []);
        this.isLoadingInvestigators.set(false);

        if (!data || data.length === 0) {
          console.warn('No SIU investigators found in response');
        }
      },
      error: (err) => {
        console.error('Error loading SIU investigators:', err);
        this.isLoadingInvestigators.set(false);
        this.siuInvestigators.set([]);

        // Show user-friendly error message
        alert('Failed to load SIU investigators. Please refresh the page and try again.');
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

        // Load fraud analysis data for each claim
        this.loadFraudAnalysisForClaims(processedClaims);
      },
      error: (error) => {
        console.error('Error loading claims:', error);
        this.errorMessage.set('Failed to load claims');
        this.isLoading.set(false);
      }
    });
  }

  /**
   * Load fraud analysis data for all claims and merge with claim data
   */
  private loadFraudAnalysisForClaims(claims: Claim[]): void {
    let completedRequests = 0;
    const totalRequests = claims.length;
    const claimsWithFraud: Claim[] = [];

    if (totalRequests === 0) {
      this.claims.set([]);
      this.filteredClaims.set([]);
      this.isLoading.set(false);
      return;
    }

    claims.forEach((claim, index) => {
      // Try to get fraud analysis for each claim
      this.fraudService.getFraudAnalysis(claim.claimId).subscribe({
        next: (fraudData) => {
          // Merge fraud analysis data into claim
          const enrichedClaim: Claim = {
            ...claim,
            fraudScore: fraudData.fraudScore,
            riskLevel: fraudData.riskLevel,
            fraudStatus: fraudData.fraudStatus,
            fraudAnalysisTimestamp: fraudData.analysisTimestamp
          };
          claimsWithFraud[index] = enrichedClaim;

          // Check completion in success case
          completedRequests++;
          this.checkFraudAnalysisCompletion(claimsWithFraud, completedRequests, totalRequests);
        },
        error: (error) => {
          console.warn(`No fraud analysis for claim ${claim.claimId}:`, error);
          // Keep original claim with default fraud values
          claimsWithFraud[index] = {
            ...claim,
            fraudScore: 0,
            riskLevel: 'LOW',
            fraudStatus: 'CLEAR'
          };

          // Check completion in error case
          completedRequests++;
          this.checkFraudAnalysisCompletion(claimsWithFraud, completedRequests, totalRequests);
        }
      });
    });
  }

  /**
   * Helper method to check if fraud analysis loading is complete
   */
  private checkFraudAnalysisCompletion(claimsWithFraud: Claim[], completedRequests: number, totalRequests: number): void {
    if (completedRequests === totalRequests) {
      // All fraud analysis requests completed, update UI
      this.claims.set(claimsWithFraud);
      this.filteredClaims.set(claimsWithFraud);
      this.isLoading.set(false);
      console.log('✅ Claims loaded with fraud analysis:', claimsWithFraud);
    }
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
        alert('Claim approved successfully!');
      },
      error: (error) => {
        console.error('Error approving claim:', error);
        alert('Failed to approve claim. Please try again.');
      }
    });
  }

  rejectClaim(id: number): void {
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
        alert('Claim rejected successfully!');
      },
      error: (error) => {
        console.error('Error rejecting claim:', error);
        alert('Failed to reject claim. Please try again.');
      }
    });
  }

  assignInspection(claim: Claim): void {
    const claimId = claim.claimId;
    const surveyorId = this.selectedSurveyors[claimId];
    if (!surveyorId) {
      alert('Please select a surveyor first');
      return;
    }
    
    this.adminService.assignClaimToSurveyor(claimId, surveyorId).subscribe({
      next: (insResponse) => {
        // Update local claim state visually 
        const updated = this.claims().map(c => {
          if (c.claimId === claimId) {
            return { ...c, status: 'INSPECTING' as any }; 
          }
          return c;
        });
        
        this.claims.set(updated);
        this.filteredClaims.set(updated);
        alert('Claim inspection assigned successfully');
      },
      error: (err) => {
        console.error('Error assigning claim inspection:', err);
        const msg = err.error?.message || err.message || 'Failed to assign claim inspection';
        alert(msg);
      }
    });
  }

  assignUnderwriter(claim: Claim): void {
    const claimId = claim.claimId;
    const underwriterId = this.selectedUnderwriters[claimId];
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
        const msg = err.error?.message || 'Failed to assign underwriter.';
        alert(msg);
      }
    });
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

  closeDocsModal(): void {
    this.selectedClaimForDocs.set(null);
    this.claimDocuments.set([]);
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
   * Smart calculation fallback for settlement amount.
   * If settlementAmount is 0 or missing, calculate it as:
   * Math.max(0, estimatedLoss - deductible - depreciation)
   */
  private calculateSettlementAmount(claim: Claim): Claim {
    const estimatedLoss = Number(claim.estimatedLoss) || 0;
    const deductible = Number(claim.deductible) || 0;
    const depreciation = Number(claim.depreciation) || 0;
    const settlementAmount = Number(claim.settlementAmount) || 0;

    // If settlement amount is 0 or missing, calculate it
    if (settlementAmount === 0 && estimatedLoss > 0) {
      const calculatedAmount = Math.max(0, estimatedLoss - deductible - depreciation);
      return { ...claim, settlementAmount: calculatedAmount };
    }

    return claim;
  }

  // ==================== FRAUD DETECTION METHODS ====================

  /**
   * Filter claims by risk level
   */
  filterByRiskLevel(riskLevel: RiskLevel | 'ALL'): void {
    this.selectedRiskLevel.set(riskLevel);
  }

  /**
   * Update search term and trigger filtering
   */
  updateSearch(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.searchTerm.set(input.value);
  }

  /**
   * View fraud analysis for a claim
   */
  viewFraudAnalysis(claim: Claim): void {
    this.selectedClaimForFraud.set(claim);
    this.isLoadingFraud.set(true);
    this.showFraudModal.set(true);

    this.fraudService.getFraudAnalysis(claim.claimId).subscribe({
      next: (analysis) => {
        this.fraudAnalysis.set(analysis);
        this.isLoadingFraud.set(false);
      },
      error: (err) => {
        console.error('Failed to load fraud analysis:', err);
        this.isLoadingFraud.set(false);
        // Show some default data or error state
        this.fraudAnalysis.set({
          claimId: claim.claimId,
          fraudScore: claim.fraudScore || 0,
          riskLevel: claim.riskLevel || 'LOW',
          fraudStatus: claim.fraudStatus || 'CLEAR',
          ruleBreakdown: [],
          analysisTimestamp: new Date().toISOString(),
          recommendation: 'Unable to load detailed analysis.'
        });
      }
    });
  }

  /**
   * Close fraud analysis modal
   */
  closeFraudModal(): void {
    this.showFraudModal.set(false);
    this.selectedClaimForFraud.set(null);
    this.fraudAnalysis.set(null);
  }

  /**
   * Open SIU assignment modal
   */
  assignToSiu(): void {
    const investigators = this.siuInvestigators();

    // Ensure investigators are loaded before showing modal
    if (investigators.length === 0) {
      alert('Loading SIU investigators...');
      this.loadSiuInvestigators();

      // Wait for investigators to load, then retry
      setTimeout(() => {
        if (this.siuInvestigators().length > 0) {
          this.assignToSiu(); // Retry after loading
        } else {
          alert('No SIU investigators available. Please contact an administrator.');
        }
      }, 2000);
      return;
    }

    this.showSiuModal.set(true);

    // Use first available investigator as default (now guaranteed to exist)
    this.siuAssignment.set({
      siuInvestigatorId: investigators[0].investigatorId,
      initialNotes: ''
    });
  }

  /**
   * Close SIU assignment modal
   */
  closeSiuModal(): void {
    this.showSiuModal.set(false);
  }

  /**
   * Update SIU investigator ID
   */
  updateSiuInvestigator(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.siuAssignment.update(assignment => ({
      ...assignment,
      siuInvestigatorId: parseInt(select.value)
    }));
  }

  /**
   * Update SIU initial notes
   */
  updateSiuNotes(event: Event): void {
    const textarea = event.target as HTMLTextAreaElement;
    this.siuAssignment.update(assignment => ({
      ...assignment,
      initialNotes: textarea.value
    }));
  }

  /**
   * Confirm SIU assignment
   */
  confirmSiuAssignment(): void {
    const claim = this.selectedClaimForFraud();
    const assignment = this.siuAssignment();
    const investigators = this.siuInvestigators();

    // Comprehensive validation before API call
    if (!claim) {
      alert('No claim selected for SIU assignment');
      return;
    }

    // Check if claim is already assigned to SIU investigation
    if (claim.fraudStatus === 'SIU_INVESTIGATION') {
      alert('This claim is already assigned to SIU investigation. Please check the SIU investigator dashboard for details.');
      return;
    }

    if (investigators.length === 0) {
      alert('No SIU investigators available. Please ensure investigators are loaded.');
      this.loadSiuInvestigators(); // Retry loading
      return;
    }

    if (!assignment.siuInvestigatorId || assignment.siuInvestigatorId <= 0) {
      alert('Please select a valid SIU investigator');
      return;
    }

    // Verify selected investigator exists in loaded list
    const selectedInvestigator = investigators.find(inv => inv.investigatorId === assignment.siuInvestigatorId);
    if (!selectedInvestigator) {
      alert('Selected investigator is not available. Please refresh and try again.');
      return;
    }

    console.log(`Assigning claim ${claim.claimId} to investigator ${selectedInvestigator.username} (ID: ${assignment.siuInvestigatorId})`);

    this.adminService.assignClaimToSiu(claim.claimId, assignment).subscribe({
      next: (response) => {
        // Update local claim state using the response data
        this.claims.update(claims =>
          claims.map(c =>
            c.claimId === response.claimId
              ? {
                  ...c,
                  fraudStatus: response.fraudStatus as any,
                  siuInvestigatorId: response.siuInvestigatorId
                }
              : c
          )
        );

        this.closeSiuModal();
        this.closeFraudModal();
        alert(response.message || 'Claim successfully assigned to SIU investigator');
      },
      error: (err) => {
        console.error('Failed to assign to SIU:', err);
        console.error('Error details:', {
          status: err.status,
          statusText: err.statusText,
          error: err.error,
          message: err.message
        });

        let errorMessage = 'Failed to assign claim to SIU investigator';

        // Extract specific backend error message
        // Backend returns SiuAssignmentResponse with message field
        if (err.error?.message) {
          errorMessage = err.error.message;
        } else if (err.error && typeof err.error === 'string') {
          errorMessage = `Assignment failed: ${err.error}`;
        } else if (err.message) {
          errorMessage = `Assignment failed: ${err.message}`;
        } else if (err.status === 400) {
          errorMessage = 'Invalid assignment data. This claim may already have an investigation case.';
        } else if (err.status === 403) {
          errorMessage = 'You do not have permission to assign claims to SIU investigators.';
        } else if (err.status === 500) {
          errorMessage = 'Server error during assignment. Please try again or contact support.';
        }

        alert(errorMessage);
      }
    });
  }

  // ==================== FRAUD UI HELPER METHODS ====================

  /**
   * Get fraud score color based on risk level
   */
  getFraudScoreColor(riskLevel?: RiskLevel): string {
    switch (riskLevel) {
      case 'CRITICAL': return 'bg-red-500';
      case 'HIGH': return 'bg-orange-500';
      case 'MEDIUM': return 'bg-yellow-500';
      case 'LOW': return 'bg-green-500';
      default: return 'bg-gray-300';
    }
  }

  /**
   * Get risk level badge classes
   */
  getRiskLevelClass(riskLevel?: RiskLevel): string {
    switch (riskLevel) {
      case 'CRITICAL': return 'bg-red-100 text-red-800 border-red-200';
      case 'HIGH': return 'bg-orange-100 text-orange-800 border-orange-200';
      case 'MEDIUM': return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'LOW': return 'bg-green-100 text-green-800 border-green-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  }

  /**
   * Get fraud status badge classes
   */
  getFraudStatusClass(fraudStatus?: FraudStatus): string {
    switch (fraudStatus) {
      case 'CONFIRMED_FRAUD': return 'bg-red-100 text-red-800 border-red-200';
      case 'SIU_INVESTIGATION': return 'bg-purple-100 text-purple-800 border-purple-200';
      case 'FLAGGED': return 'bg-orange-100 text-orange-800 border-orange-200';
      case 'UNDER_REVIEW': return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'CLEARED': return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'CLEAR': return 'bg-green-100 text-green-800 border-green-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  }

  /**
   * Format fraud status for display
   */
  formatFraudStatus(fraudStatus?: FraudStatus): string {
    switch (fraudStatus) {
      case 'SIU_INVESTIGATION': return 'SIU Investigation';
      case 'UNDER_REVIEW': return 'Under Review';
      case 'CONFIRMED_FRAUD': return 'Confirmed Fraud';
      default: return fraudStatus || 'Clear';
    }
  }

  /**
   * Get risk level filter classes
   */
  getRiskFilterClass(riskLevel: RiskLevel | 'ALL'): string {
    const isSelected = this.selectedRiskLevel() === riskLevel;

    if (riskLevel === 'ALL') {
      return isSelected
        ? 'bg-gray-800 text-white'
        : 'bg-gray-100 text-gray-600 hover:bg-gray-200';
    }

    const baseClasses = isSelected ? 'text-white' : 'hover:opacity-80';

    switch (riskLevel) {
      case 'CRITICAL':
        return isSelected ? `${baseClasses} bg-red-600` : `${baseClasses} bg-red-50 text-red-600`;
      case 'HIGH':
        return isSelected ? `${baseClasses} bg-orange-600` : `${baseClasses} bg-orange-50 text-orange-600`;
      case 'MEDIUM':
        return isSelected ? `${baseClasses} bg-yellow-600` : `${baseClasses} bg-yellow-50 text-yellow-600`;
      case 'LOW':
        return isSelected ? `${baseClasses} bg-green-600` : `${baseClasses} bg-green-50 text-green-600`;
      default:
        return 'bg-gray-100 text-gray-600';
    }
  }

  /**
   * Get count for risk level filter
   */
  getRiskLevelCount(riskLevel: RiskLevel | 'ALL'): number {
    const claims = this.claims();

    if (riskLevel === 'ALL') {
      return claims.length;
    }

    return claims.filter(claim => claim.riskLevel === riskLevel).length;
  }
}
