import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';
import { SiuService, SiuClaimDetails, SiuInvestigationActionResponse } from '../../services/siu.service';
import { FraudEngineService, FraudAnalysisResult } from '../../services/fraud-engine.service';
import { DocumentService } from '../../../../core/services/document.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-siu-claim-details',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './siu-claim-details.component.html'
})
export class SiuClaimDetailsComponent implements OnInit {
  private readonly siuService = inject(SiuService);
  private readonly fraudEngine = inject(FraudEngineService);
  private readonly documentService = inject(DocumentService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly location = inject(Location);

  // Component state
  claimDetails = signal<SiuClaimDetails | null>(null);
  isLoading = signal(true);
  errorMessage = signal('');
  claimId = signal('');

  // Analysis state
  isAnalyzing = signal(false);
  analysisResult = signal<FraudAnalysisResult | null>(null);
  documents = signal<any[]>([]);

  // Investigation action loading states
  isStartingInvestigation = signal(false);
  isMarkingFraud = signal(false);
  isClearingClaim = signal(false);
  actionMessage = signal('');
  actionSuccess = signal<boolean | null>(null);

  // Computed properties
  fraudRiskLevel = computed(() => {
    const claim = this.claimDetails();
    if (!claim || typeof claim.fraudScore !== 'number') return 'unknown';

    if (claim.fraudScore >= 80) return 'high';
    if (claim.fraudScore >= 50) return 'medium';
    return 'low';
  });

  suspiciousIndicators = computed(() => {
    const claim = this.claimDetails();
    if (!claim || typeof claim.fraudScore !== 'number') return ['Backend data unavailable'];

    const indicators: string[] = [];

    // Generate suspicious indicators based on claim data
    if (claim.fraudScore >= 80) {
      indicators.push('High fraud score detected');
    }

    if (claim.claimAmount && claim.claimAmount > 500000) {
      indicators.push('High claim amount');
    }

    if (claim.claimAmount && claim.policy?.maxCoverageAmount && claim.claimAmount > claim.policy.maxCoverageAmount) {
      indicators.push('Claim exceeds policy coverage');
    }

    if (claim.settlementAmount === 0 && claim.status === 'REJECTED') {
      indicators.push('Previously rejected claim');
    }

    if (!claim.firNumber && claim.causeOfFire?.toLowerCase().includes('fire')) {
      indicators.push('Missing FIR number for fire incident');
    }

    return indicators.length > 0 ? indicators : ['Routine investigation'];
  });

  customerDisplayName = computed(() => {
    const claim = this.claimDetails();
    if (!claim?.customer) return 'Unknown Customer';

    return `${claim.customer.firstName || ''} ${claim.customer.lastName || ''}`.trim() ||
           claim.customer.email ||
           'Unknown Customer';
  });

  // Investigation action validation computed properties
  canStartInvestigation = computed(() => {
    const claim = this.claimDetails();
    if (!claim || !claim.status) return false;

    // Can start investigation if claim is submitted, under review, or survey completed
    const validStatuses = ['SUBMITTED', 'UNDER_REVIEW', 'SURVEY_COMPLETED'];
    return validStatuses.includes(claim.status.toUpperCase()) &&
           !this.isStartingInvestigation() &&
           !this.isPerformingAnyAction();
  });

  canMarkAsFraud = computed(() => {
    const claim = this.claimDetails();
    const result = this.analysisResult();
    if (!claim || !claim.status || !result) return false;
 
    // Must have a HIGH risk score (>= 70) to mark as fraud
    const isHighRisk = result.totalScore >= 70;
 
    const validStatuses = ['UNDER_REVIEW', 'SURVEY_COMPLETED', 'SURVEY_ASSIGNED'];
    return validStatuses.includes(claim.status.toUpperCase()) &&
           isHighRisk &&
           !this.isMarkingFraud() &&
           !this.isPerformingAnyAction();
  });

  canClearClaim = computed(() => {
    const claim = this.claimDetails();
    const result = this.analysisResult();
    if (!claim || !claim.status || !result) return false;
 
    // Must have a LOW or MEDIUM risk score (< 70) to clear claim
    const isLowMediumRisk = result.totalScore < 70;
 
    const validStatuses = ['UNDER_REVIEW', 'SURVEY_COMPLETED', 'SURVEY_ASSIGNED', 'SUBMITTED'];
    return validStatuses.includes(claim.status.toUpperCase()) &&
           isLowMediumRisk &&
           !this.isClearingClaim() &&
           !this.isPerformingAnyAction();
  });

  isPerformingAnyAction = computed(() => {
    return this.isStartingInvestigation() || this.isMarkingFraud() || this.isClearingClaim() || this.isAnalyzing();
  });

  investigationStatusMessage = computed(() => {
    const claim = this.claimDetails();
    if (!claim || !claim.status) return 'Claim data not available';

    switch (claim.status.toUpperCase()) {
      case 'SUBMITTED':
        return 'Claim submitted - Ready for investigation';
      case 'UNDER_REVIEW':
        return 'Investigation in progress';
      case 'SURVEY_ASSIGNED':
        return 'Survey assigned - Investigation pending';
      case 'SURVEY_COMPLETED':
        return 'Survey completed - Ready for SIU decision';
      case 'APPROVED':
        return 'Claim approved - Investigation closed';
      case 'REJECTED':
        return 'Claim rejected - Investigation closed';
      case 'PAID':
        return 'Claim settled - Investigation completed';
      default:
        return 'Status unknown';
    }
  });

  ngOnInit(): void {
    // Get claim ID from route parameters
    const claimId = this.route.snapshot.paramMap.get('id');
    if (claimId) {
      this.claimId.set(claimId);
      this.loadClaimDetails(claimId);
      this.loadDocuments(claimId);
    } else {
      this.errorMessage.set('No claim ID provided in route');
      this.isLoading.set(false);
    }
  }

  /**
   * Load claim details from backend API
   */
  private loadClaimDetails(claimId: string): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.siuService.getClaimDetails(claimId).subscribe({
      next: (claimDetails: SiuClaimDetails) => {
        // Check if backend returned placeholder response
        if ((claimDetails as any).message && (claimDetails as any).message.includes('Implementation pending')) {
          this.errorMessage.set('Backend claim details endpoint is not yet implemented. Please implement the SIU claim details API endpoint.');
          this.claimDetails.set(null);
        } else {
          this.claimDetails.set(claimDetails);
        }
        this.isLoading.set(false);
        console.log('Claim details loaded:', claimDetails);
      },
      error: (error: HttpErrorResponse) => {
        console.error('Error loading claim details:', error);
        this.errorMessage.set(this.getApiErrorMessage(error));
        this.isLoading.set(false);
      }
    });
  }

  /**
   * Load documents associated with this claim for cross-validation
   */
  private loadDocuments(claimId: string): void {
    this.documentService.getDocumentsForEntity(Number(claimId), 'CLAIM').subscribe({
      next: (docs) => this.documents.set(docs || []),
      error: (err) => console.error('Failed to load documents for fraud analysis', err)
    });
  }

  /**
   * Executes the AI-Inspired Smart Scan via the Fraud Engine
   */
  runSmartScan(): void {
    const claim = this.claimDetails();
    if (!claim) return;

    this.isAnalyzing.set(true);
    this.analysisResult.set(null);

    // Simulate "Deep Analysis" processing time for better UX
    setTimeout(() => {
      const result = this.fraudEngine.analyzeClaim(claim, this.documents());
      this.analysisResult.set(result);
      this.isAnalyzing.set(false);
      
      // If score is high, optionally update local fraudScore for UI consistency
      if (result.totalScore > (claim.fraudScore || 0)) {
        this.claimDetails.set({ ...claim, fraudScore: result.totalScore });
      }
    }, 1500);
  }

  /**
   * Navigate back to SIU dashboard
   */
  goBack(): void {
    this.location.back();
  }

  /**
   * Navigate back to SIU dashboard (alternative method)
   */
  backToDashboard(): void {
    this.router.navigate(['/siu-dashboard/dashboard']);
  }

  /**
   * Get CSS class for fraud score display
   */
  getFraudScoreClass(score: number): string {
    if (score >= 80) return 'text-red-600 font-bold';
    if (score >= 50 && score < 80) return 'text-orange-600 font-semibold';
    return 'text-green-600';
  }

  /**
   * Get CSS class for fraud risk level
   */
  getFraudRiskClass(): string {
    const level = this.fraudRiskLevel();
    switch (level) {
      case 'high': return 'bg-red-100 text-red-800 border-red-200';
      case 'medium': return 'bg-orange-100 text-orange-800 border-orange-200';
      case 'low': return 'bg-green-100 text-green-800 border-green-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  }

  /**
   * Get CSS class for claim state badge
   */
  getStateBadgeClass(state: string): string {
    switch (state?.toUpperCase()) {
      case 'SUBMITTED':
        return 'bg-blue-100 text-blue-800 border border-blue-200';
      case 'UNDER_REVIEW':
        return 'bg-yellow-100 text-yellow-800 border border-yellow-200';
      case 'SURVEY_ASSIGNED':
      case 'SURVEY_COMPLETED':
        return 'bg-purple-100 text-purple-800 border border-purple-200';
      case 'APPROVED':
        return 'bg-green-100 text-green-800 border border-green-200';
      case 'REJECTED':
        return 'bg-red-100 text-red-800 border border-red-200';
      case 'PAID':
        return 'bg-emerald-100 text-emerald-800 border border-emerald-200';
      default:
        return 'bg-gray-100 text-gray-800 border border-gray-200';
    }
  }

  /**
   * Format currency values
   */
  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount);
  }

  /**
   * Format date values
   */
  formatDate(dateString: string): string {
    try {
      return new Date(dateString).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
    } catch {
      return dateString;
    }
  }

  /**
   * Update claim investigation status
   */
  updateClaimStatus(newStatus: string): void {
    const claimId = this.claimId();
    if (!claimId) return;

    this.siuService.updateClaimStatus(claimId, newStatus).subscribe({
      next: () => {
        console.log('Claim status updated:', claimId, newStatus);
        // Reload claim details to get updated information
        this.loadClaimDetails(claimId);
      },
      error: (error) => {
        console.error('Error updating claim status:', error);
        this.errorMessage.set('Failed to update claim status.');
      }
    });
  }

  // === Investigation Action Methods ===

  /**
   * Start formal SIU investigation
   */
  startInvestigation(): void {
    const claimId = this.claimId();
    if (!claimId || !this.canStartInvestigation()) return;

    this.clearActionMessages();
    this.isStartingInvestigation.set(true);

    this.siuService.startInvestigation(claimId, 'Formal SIU investigation initiated').subscribe({
      next: (response: SiuInvestigationActionResponse) => {
        console.log('Investigation started:', response);
        this.handleActionSuccess(response, 'Investigation started successfully');
        // Reload claim details to show updated status
        this.loadClaimDetails(claimId);
      },
      error: (error: HttpErrorResponse) => {
        console.error('Error starting investigation:', error);
        this.handleActionError('Failed to start investigation', error);
      },
      complete: () => {
        this.isStartingInvestigation.set(false);
      }
    });
  }

  /**
   * Mark claim as fraudulent
   */
  markAsFraud(): void {
    const claimId = this.claimId();
    if (!claimId || !this.canMarkAsFraud()) return;

    const claim = this.claimDetails();
    const reason = `Fraudulent claim confirmed. Fraud score: ${claim?.fraudScore}%. Suspicious indicators detected.`;

    this.clearActionMessages();
    this.isMarkingFraud.set(true);

    this.siuService.markAsFraud(claimId, reason).subscribe({
      next: (response: SiuInvestigationActionResponse) => {
        console.log('Claim marked as fraud:', response);
        this.handleActionSuccess(response, 'Claim successfully marked as fraudulent');
        // Reload claim details to show updated status
        this.loadClaimDetails(claimId);
      },
      error: (error: HttpErrorResponse) => {
        console.error('Error marking as fraud:', error);
        this.handleActionError('Failed to mark claim as fraudulent', error);
      },
      complete: () => {
        this.isMarkingFraud.set(false);
      }
    });
  }

  /**
   * Clear claim as legitimate
   */
  clearClaim(): void {
    const claimId = this.claimId();
    if (!claimId || !this.canClearClaim()) return;

    const notes = 'Claim cleared as legitimate after SIU investigation. No fraudulent activity detected.';

    this.clearActionMessages();
    this.isClearingClaim.set(true);

    this.siuService.clearClaim(claimId, notes).subscribe({
      next: (response: SiuInvestigationActionResponse) => {
        console.log('Claim cleared:', response);
        this.handleActionSuccess(response, 'Claim successfully cleared as legitimate');
        // Reload claim details to show updated status
        this.loadClaimDetails(claimId);
      },
      error: (error: HttpErrorResponse) => {
        console.error('Error clearing claim:', error);
        this.handleActionError('Failed to clear claim', error);
      },
      complete: () => {
        this.isClearingClaim.set(false);
      }
    });
  }

  /**
   * Handle successful investigation actions
   */
  private handleActionSuccess(response: SiuInvestigationActionResponse, userMessage: string): void {
    this.actionSuccess.set(true);
    this.actionMessage.set(userMessage);
    console.log('Action response:', response);

    // Auto-clear success message after 5 seconds
    setTimeout(() => {
      this.clearActionMessages();
    }, 5000);
  }

  /**
   * Handle investigation action errors
   */
  private handleActionError(userMessage: string, error: HttpErrorResponse): void {
    this.actionSuccess.set(false);
    const errorDetail = error.error?.message || error.message || 'Unknown error occurred';
    this.actionMessage.set(`${userMessage}: ${errorDetail}`);
  }

  /**
   * Clear action messages and status
   */
  private clearActionMessages(): void {
    this.actionMessage.set('');
    this.actionSuccess.set(null);
  }

  /**
   * Get user-friendly error message from HTTP error
   */
  private getApiErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'Cannot connect to API server. Please ensure the backend is running.';
    }
    if (error.status === 403) {
      return 'Access denied. SIU_INVESTIGATOR role required to access this claim.';
    }
    if (error.status === 404) {
      return `Claim ${this.claimId()} not found. It may have been deleted or you may not have access.`;
    }
    if (error.status === 500) {
      return 'Server error occurred while loading claim details. Please try again later.';
    }
    return error.error?.message || error.message || 'Failed to load claim details.';
  }

  /**
   * Retry loading claim details
   */
  retryLoad(): void {
    const claimId = this.claimId();
    if (claimId) {
      this.loadClaimDetails(claimId);
    }
  }
}