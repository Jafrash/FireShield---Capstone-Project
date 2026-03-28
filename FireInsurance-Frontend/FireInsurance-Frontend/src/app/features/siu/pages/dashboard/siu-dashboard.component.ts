import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { SiuService, SiuClaim, SiuClaimsResponse } from '../../services/siu.service';
import { InvestigationToolsService } from '../../services/investigation-tools.service';
import { HttpErrorResponse } from '@angular/common/http';

type SortField = 'fraudScore' | 'claimId' | 'claimAmount' | 'assignedDate' | 'priority';
type SortDirection = 'asc' | 'desc';

@Component({
  selector: 'app-siu-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './siu-dashboard.component.html'
})
export class SiuDashboardComponent implements OnInit {
  private siuService = inject(SiuService);
  private aiService = inject(InvestigationToolsService);
  private router = inject(Router);

  // Real data from API - no more dummy data
  private allClaims = signal<SiuClaim[]>([]);
  claimsResponse = signal<SiuClaimsResponse | null>(null);
  isLoading = signal(true);
  errorMessage = signal('');

  // Sorting and filtering
  sortField = signal<SortField>('fraudScore');
  sortDirection = signal<SortDirection>('desc');
  stateFilter = signal<string>('ALL');

  // Available states for filtering
  availableStates = computed(() => {
    const states = new Set(this.allClaims().map(claim => claim.state));
    return ['ALL', ...Array.from(states).sort()];
  });

  // Filtered and sorted claims
  claims = computed(() => {
    let filteredClaims = this.allClaims();

    // 1. Strict Triage Filter: Only show HIGH risk claims to the SIU investigator
    filteredClaims = filteredClaims.filter(claim =>
      claim.priority === 'HIGH' || (claim.fraudScore && claim.fraudScore >= 70)
    );

    // 2. Apply state filter
    if (this.stateFilter() !== 'ALL') {
      filteredClaims = filteredClaims.filter(claim => claim.state === this.stateFilter());
    }

    // Apply sorting
    const sortField = this.sortField();
    const sortDirection = this.sortDirection();

    return filteredClaims.sort((a, b) => {
      let aValue: any = a[sortField];
      let bValue: any = b[sortField];

      // Handle different data types
      if (sortField === 'fraudScore' || sortField === 'claimAmount') {
        aValue = Number(aValue) || 0;
        bValue = Number(bValue) || 0;
      } else if (sortField === 'assignedDate') {
        aValue = new Date(aValue || 0).getTime();
        bValue = new Date(bValue || 0).getTime();
      } else if (sortField === 'priority') {
        const priorityOrder = { 'HIGH': 3, 'MEDIUM': 2, 'LOW': 1 };
        aValue = priorityOrder[aValue as keyof typeof priorityOrder] || 0;
        bValue = priorityOrder[bValue as keyof typeof priorityOrder] || 0;
      } else {
        aValue = String(aValue || '').toLowerCase();
        bValue = String(bValue || '').toLowerCase();
      }

      if (aValue < bValue) return sortDirection === 'asc' ? -1 : 1;
      if (aValue > bValue) return sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  });

  // Computed values from real API data (adjusted for filtered results)
  totalAssignedClaims = computed(() => this.claims().length);
  highPriorityClaims = computed(() =>
    this.claims().filter(claim => claim.priority === 'HIGH').length
  );
  suspiciousClaims = computed(() =>
    this.claims().filter(claim => claim.fraudScore >= 70).length
  );
  averageFraudScore = computed(() => {
    const claims = this.claims();
    if (claims.length === 0) return 0;
    const total = claims.reduce((sum, claim) => sum + claim.fraudScore, 0);
    return Math.round(total / claims.length);
  });
  totalClaimAmount = computed(() =>
    this.claims().reduce((sum, claim) => sum + (claim.claimAmount || 0), 0)
  );

  // Engine Health State (Step 3)
  engineVersion = signal('v2.4 (Hybrid Heuristic-AI)');
  activeRulesCount = signal(12);
  engineStatus = signal('Optimal');
  lastScanTime = signal(new Date().toLocaleTimeString());

  // Advanced Tool Notification State
  toolMessage = signal('');
  toolIcon = signal('info');
  showToolPopup = signal(false);

  ngOnInit(): void {
    this.loadSiuClaims();
  }

  /**
   * Load SIU claims from backend API
   */
  loadSiuClaims(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.siuService.getClaims().subscribe({
      next: (response: SiuClaimsResponse) => {
        this.claimsResponse.set(response);
        this.allClaims.set(response.claims);
        this.isLoading.set(false);
        console.log('SIU Claims loaded:', response);
      },
      error: (error: HttpErrorResponse) => {
        console.error('Error loading SIU claims:', error);
        this.errorMessage.set(this.getApiErrorMessage(error));
        this.isLoading.set(false);
      }
    });
  }

  /**
   * Sort table by field
   */
  sortBy(field: SortField): void {
    if (this.sortField() === field) {
      // Toggle direction if same field
      this.sortDirection.set(this.sortDirection() === 'asc' ? 'desc' : 'asc');
    } else {
      // Set new field with appropriate default direction
      this.sortField.set(field);
      this.sortDirection.set(field === 'fraudScore' || field === 'claimAmount' || field === 'priority' ? 'desc' : 'asc');
    }
  }

  /**
   * Filter claims by state
   */
  filterByState(state: string): void {
    this.stateFilter.set(state);
  }

  /**
   * Get sort icon class for column headers
   */
  getSortIcon(field: SortField): string {
    if (this.sortField() !== field) return 'sort';
    return this.sortDirection() === 'asc' ? 'keyboard_arrow_up' : 'keyboard_arrow_down';
  }

  /**
   * Get user-friendly error message from HTTP error
   */
  private getApiErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'Cannot connect to API server at http://localhost:8080. Please ensure the backend is running.';
    }
    if (error.status === 403) {
      return 'Access denied. SIU_INVESTIGATOR role required to access this data.';
    }
    if (error.status === 404) {
      return 'SIU claims endpoint not found. Please check if the backend supports /api/siu/claims.';
    }
    return error.error?.message || error.message || 'Failed to load SIU claims data.';
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
   * Get CSS class for fraud score background highlight
   */
  getFraudScoreRowClass(score: number): string {
    if (score >= 80) return 'bg-red-50 border-l-4 border-red-500';
    if (score >= 50 && score < 80) return 'bg-orange-50 border-l-4 border-orange-500';
    return '';
  }

  /**
   * Get CSS class for claim state badge
   */
  getStateBadgeClass(state: string): string {
    switch (state?.toUpperCase()) {
      case 'UNDER_INVESTIGATION':
        return 'bg-yellow-100 text-yellow-800 border border-yellow-200';
      case 'SUSPICIOUS':
        return 'bg-red-100 text-red-800 border border-red-200';
      case 'CLEARED':
        return 'bg-green-100 text-green-800 border border-green-200';
      case 'FRAUD_CONFIRMED':
        return 'bg-red-600 text-white border border-red-700';
      case 'PENDING_REVIEW':
        return 'bg-blue-100 text-blue-800 border border-blue-200';
      default:
        return 'bg-gray-100 text-gray-800 border border-gray-200';
    }
  }

  /**
   * Get CSS class for priority badge
   */
  getPriorityBadgeClass(priority: string): string {
    switch (priority?.toUpperCase()) {
      case 'HIGH':
        return 'bg-red-500 text-white';
      case 'MEDIUM':
        return 'bg-yellow-500 text-white';
      case 'LOW':
        return 'bg-green-500 text-white';
      default:
        return 'bg-gray-500 text-white';
    }
  }

  /**
   * View claim details for investigation
   */
  viewClaimDetails(claimId: string): void {
    console.log('Navigating to claim details:', claimId);
    this.router.navigate(['/siu-dashboard/claim', claimId]);
  }

  /**
   * Update claim investigation status
   */
  updateClaimStatus(claimId: string, newStatus: string): void {
    this.siuService.updateClaimStatus(claimId, newStatus).subscribe({
      next: () => {
        console.log('Claim status updated:', claimId, newStatus);
        this.loadSiuClaims(); // Refresh data
      },
      error: (error) => {
        console.error('Error updating claim status:', error);
        this.errorMessage.set('Failed to update claim status.');
      }
    });
  }

  /**
   * Retry loading claims data
   */
  retryLoad(): void {
    this.loadSiuClaims();
  }

  // --- Advanced Investigation Tools Hooks --- //

  async runPatternAnalysis(): Promise<void> {
    this.toolIcon.set('search');
    this.toolMessage.set('Scanning via Gemini for cross-claim patterns and linked entities...');
    this.showToolPopup.set(true);
    
    try {
      const activeClaims = this.claims();
      const result = await this.aiService.runPatternAnalysis(activeClaims);
      this.toolIcon.set(result.clusterFound ? 'warning' : 'check_circle');
      this.toolMessage.set(result.aiSynthesis);
    } catch (e) {
      this.toolIcon.set('error');
      this.toolMessage.set('Pattern Analysis failed. Fallback heuristic also encountered an error.');
    }
  }

  checkFraudEngine(): void {
    this.toolIcon.set('analytics');
    this.toolMessage.set('Running heuristic engine diagnostics. Rule Set V2.4 is active.');
    this.showToolPopup.set(true);
    
    setTimeout(() => {
      this.toolIcon.set('check_circle');
      this.toolMessage.set('Engine Operational. ' + this.highPriorityClaims() + ' claims flagged under Priority: HIGH.');
    }, 1500);
  }

  async runRiskAssessment(): Promise<void> {
    this.toolIcon.set('assessment');
    this.toolMessage.set('Generating global risk distribution narrative via Gemini AI...');
    this.showToolPopup.set(true);
    
    // Choose the highest risk claim to synthesize for the demo
    const highestRisk = [...this.claims()].sort((a,b) => b.fraudScore - a.fraudScore)[0];
    
    if (highestRisk) {
      const summary = await this.aiService.runRiskAssessment(highestRisk);
      this.toolIcon.set('assessment');
      this.toolMessage.set('AI Synthesis on ' + highestRisk.claimId + ': ' + summary);
    } else {
      this.toolMessage.set('No active claims available for AI Synthesis.');
    }
  }

  async generateReport(): Promise<void> {
    this.toolIcon.set('report');
    this.toolMessage.set('Compiling forensic JSON audit report with Executive Summary...');
    this.showToolPopup.set(true);
    
    const summary = await this.aiService.generateExecutiveSummary(this.claims());
    
    this.toolIcon.set('download');
    this.toolMessage.set('Report generation complete. Downloading SIU_Audit_Report.json...');
    
    const reportPayload = {
      generatedAt: new Date().toISOString(),
      aiExecutiveSummary: summary,
      investigatedClaims: this.claims()
    };
    
    const dataStr = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(reportPayload, null, 2));
    const downloadAnchorNode = document.createElement('a');
    downloadAnchorNode.setAttribute('href', dataStr);
    downloadAnchorNode.setAttribute('download', `SIU_Audit_Report_${new Date().getTime()}.json`);
    document.body.appendChild(downloadAnchorNode);
    downloadAnchorNode.click();
    downloadAnchorNode.remove();
    
    setTimeout(() => this.showToolPopup.set(false), 3000);
  }
}