import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { UnderwriterService, UwSubscription, UwClaim } from '../../services/underwriter.service';

@Component({
  selector: 'app-uw-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './uw-dashboard.component.html'
})
export class UwDashboardComponent implements OnInit {
  private uwService = inject(UnderwriterService);

  subscriptions = signal<UwSubscription[]>([]);
  claims = signal<UwClaim[]>([]);
  isLoading = signal(true);
  errorMessage = signal('');

  assignedSubscriptions = computed(() => this.subscriptions().length);
  assignedClaims = computed(() => this.claims().length);

  // Enhanced computed properties for claims with fraud/risk analysis
  highRiskClaims = computed(() =>
    this.claims().filter(c => this.getClaimRiskLevel(c) === 'HIGH' || this.getClaimRiskLevel(c) === 'CRITICAL').length
  );

  fraudSuspiciousClaims = computed(() =>
    this.claims().filter(c => (c.fraudScore ?? 0) >= 70).length
  );

  siuBlockedClaims = computed(() =>
    this.claims().filter(c => c.siuStatus === 'UNDER_INVESTIGATION').length
  );

  availableClaims = computed(() =>
    this.claims().filter(c => c.siuStatus !== 'UNDER_INVESTIGATION').length
  );

  pendingInspections = computed(() =>
    this.subscriptions().filter(s =>
      s.status === 'REQUESTED' || s.status === 'PENDING' || s.status === 'INSPECTING'
    ).length
  );

  pendingDecisions = computed(() => {
    const pendingSubs = this.subscriptions().filter(s => s.status === 'INSPECTED').length;
    const pendingClaims = this.claims().filter(c => c.status === 'INSPECTED' && c.siuStatus !== 'UNDER_INVESTIGATION').length;
    return pendingSubs + pendingClaims;
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.uwService.getAssignedSubscriptions().subscribe({
      next: (data) => {
        this.subscriptions.set(data || []);
        this.checkLoaded();
      },
      error: (err) => {
        console.error('Error loading subscriptions', err);
        this.errorMessage.set(this.getApiErrorMessage(err, 'Failed to load subscriptions.'));
        this.checkLoaded();
      }
    });

    this.uwService.getAssignedClaims().subscribe({
      next: (data) => {
        this.claims.set(data || []);
        this.checkLoaded();
      },
      error: (err) => {
        console.error('Error loading claims', err);
        this.errorMessage.set(this.getApiErrorMessage(err, 'Failed to load claims.'));
        this.checkLoaded();
      }
    });
  }

  /**
   * Determine risk level based on fraud score and claim amount
   */
  getClaimRiskLevel(claim: UwClaim): 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' {
    if (claim.riskLevel) return claim.riskLevel;

    const fraudScore = claim.fraudScore ?? 0;
    const claimAmount = claim.claimAmount ?? 0;

    if (fraudScore >= 90 || (fraudScore >= 70 && claimAmount > 100000)) return 'CRITICAL';
    if (fraudScore >= 70 || claimAmount > 50000) return 'HIGH';
    if (fraudScore >= 40 || claimAmount > 20000) return 'MEDIUM';
    return 'LOW';
  }

  /**
   * Get risk level color for UI display
   */
  getRiskLevelColor(riskLevel: string): string {
    switch (riskLevel) {
      case 'CRITICAL': return '#DC2626'; // Red-600
      case 'HIGH': return '#EA580C'; // Orange-600
      case 'MEDIUM': return '#D97706'; // Amber-600
      case 'LOW': return '#16A34A'; // Green-600
      default: return '#6B7280'; // Gray-500
    }
  }

  /**
   * Get fraud score color for UI highlighting
   */
  getFraudScoreColor(fraudScore: number): string {
    if (fraudScore >= 90) return '#DC2626'; // Critical - Red
    if (fraudScore >= 70) return '#EA580C'; // High - Orange
    if (fraudScore >= 40) return '#D97706'; // Medium - Amber
    return '#16A34A'; // Low - Green
  }

  /**
   * Check if claim is blocked by SIU investigation
   */
  isClaimBlocked(claim: UwClaim): boolean {
    return claim.siuStatus === 'UNDER_INVESTIGATION';
  }

  private loadedCount = 0;
  private checkLoaded(): void {
    this.loadedCount++;
    if (this.loadedCount >= 2) {
      this.isLoading.set(false);
      this.loadedCount = 0;
    }
  }

  private getApiErrorMessage(error: unknown, fallback: string): string {
    const httpError = error as HttpErrorResponse;
    if (httpError?.status === 0) {
      return 'Cannot connect to API server at http://localhost:8080. Please start/restart backend and refresh.';
    }
    return (httpError?.error?.message as string) || httpError?.message || fallback;
  }

  get dashboardCards() {
    return [
      {
        title: 'Assigned Subscriptions',
        value: this.assignedSubscriptions(),
        icon: 'card_membership',
        color: '#C72B32',
        route: '/underwriter/subscriptions'
      },
      {
        title: 'Available Claims',
        value: this.availableClaims(),
        subtitle: `${this.siuBlockedClaims()} blocked by SIU`,
        icon: 'assignment',
        color: '#D81B60',
        route: '/underwriter/claims'
      },
      {
        title: 'High Risk Claims',
        value: this.highRiskClaims(),
        subtitle: `${this.fraudSuspiciousClaims()} fraud suspicious`,
        icon: 'warning',
        color: '#DC2626',
        route: '/underwriter/claims'
      },
      {
        title: 'Pending Decisions',
        value: this.pendingDecisions(),
        icon: 'pending_actions',
        color: '#E67E22',
        route: '/underwriter/subscriptions'
      }
    ];
  }
}
