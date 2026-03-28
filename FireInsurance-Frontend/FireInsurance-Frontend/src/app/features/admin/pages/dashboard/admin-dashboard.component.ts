import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AdminService } from '../../../../features/admin/services/admin.service';
import { Claim } from '../../../../core/models/claim.model';
import { PolicySubscription } from '../../../../core/models/policy.model';
import { AnalyticsCard, ADVANCED_ANALYTICS } from './analytics-cards';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-dashboard.html',
  styleUrls: ['./admin-dashboard.css']
})
export class AdminDashboardComponent implements OnInit {
  private adminService = inject(AdminService);

  dashboardCards = signal<{title: string; value: number; icon: string; color: string; route?: string}[]>([]);
  recentClaims = signal<Claim[]>([]);
  pendingSubscriptions = signal<PolicySubscription[]>([]);
  isLoading = signal(true);
  errorMessage = signal('');

  // Advanced analytics state
  analyticsCards = signal<AnalyticsCard[]>([]);

  // Raw data for analytics
  allClaims: Claim[] = [];
  allPolicies: any[] = [];
  allSubscriptions: PolicySubscription[] = [];

  ngOnInit(): void {
    this.loadDashboardData();
    this.loadAnalyticsData();
  }

  // Load all data needed for analytics
  private loadAnalyticsData(): void {
    // Claims
    this.adminService.getAllClaims().subscribe({
      next: (claims) => {
        // Apply smart calculation for settlement amounts
        this.allClaims = (claims || []).map(claim => this.calculateSettlementAmount(claim));
        this.updateAnalyticsCards();
      },
      error: () => {}
    });

    // Policies
    this.adminService.getAllPolicies().subscribe({
      next: (policies) => {
        this.allPolicies = policies || [];
        this.updateAnalyticsCards();
      },
      error: () => {}
    });

    // Subscriptions
    this.adminService.getAllSubscriptions().subscribe({
      next: (subs) => {
        this.allSubscriptions = subs || [];
        this.updateAnalyticsCards();
      },
      error: () => {}
    });
  }

  // Compute analytics metrics
  private updateAnalyticsCards(): void {
    const claims = this.allClaims;
    const policies = this.allPolicies;
    const subs = this.allSubscriptions;

    // Claims Approval Rate
    const approvedClaims = claims.filter(c => c.status === 'APPROVED').length;
    const approvalRate = claims.length > 0 ? ((approvedClaims / claims.length) * 100).toFixed(1) + '%' : '--';

    // Average Claim Amount
    const avgClaimAmount = claims.length > 0 ?
      (claims.reduce((sum, c) => sum + (c.claimAmount || 0), 0) / claims.length).toLocaleString('en-IN', { maximumFractionDigits: 0 }) : '--';


    // Active Policy Ratio
    const activePolicies = subs.filter(s => s.status === 'ACTIVE').length;
    const totalPolicies = subs.length;
    const activePolicyRatio = totalPolicies > 0 ? ((activePolicies / totalPolicies) * 100).toFixed(1) + '%' : '--';

    // Update analytics cards - removed inspections analytics
    this.analyticsCards.set([
      {
        title: 'Claims Approval Rate',
        value: approvalRate,
        icon: 'percent',
        color: '#C72B32',
        description: 'Percentage of claims approved out of total claims.'
      },
      {
        title: 'Average Claim Amount',
        value: avgClaimAmount ? `₹${avgClaimAmount}` : '--',
        icon: 'payments',
        color: '#E2725B',
        description: 'Mean value of all claims submitted.'
      },
      {
        title: 'Active Policy Ratio',
        value: activePolicyRatio,
        icon: 'pie_chart',
        color: '#FF6B35',
        description: 'Ratio of active policies to total policies.'
      }
    ]);
  }

  private loadDashboardData(): void {
    this.isLoading.set(true);

    // Load all data in parallel
    let customersCount = 0;
    let totalClaims = 0;
    let activePolicies = 0;
    let pendingSubs = 0;
    let totalPolicies = 0;
    let totalUnderwriters = 0;

    this.adminService.getAllUnderwriters().subscribe({
      next: (uws) => {
        totalUnderwriters = uws?.length || 0;
        this.updateCards(customersCount, totalClaims, activePolicies, pendingSubs, totalPolicies, totalUnderwriters);
      },
      error: () => {}
    });

    this.adminService.getAllCustomers().subscribe({
      next: (customers) => {
        customersCount = customers?.length || 0;
        this.updateCards(customersCount, totalClaims, activePolicies, pendingSubs, totalPolicies, totalUnderwriters);
      },
      error: () => {}
    });

    this.adminService.getAllClaims().subscribe({
      next: (claims) => {
        // Apply smart calculation for settlement amounts
        const processedClaims = (claims || []).map(claim => this.calculateSettlementAmount(claim));
        totalClaims = processedClaims.length;
        this.recentClaims.set(processedClaims.slice(0, 5));
        this.updateCards(customersCount, totalClaims, activePolicies, pendingSubs, totalPolicies, totalUnderwriters);
      },
      error: () => {}
    });

    this.adminService.getAllSubscriptions().subscribe({
      next: (subs) => {
        const isPending = (s: PolicySubscription) => ['PENDING', 'SUBMITTED', 'REQUESTED', 'INSPECTING', 'INSPECTED', 'INSPECTION_PENDING', 'UNDER_REVIEW', 'PAYMENT_PENDING'].includes(s.status);
        activePolicies = subs?.filter((s: PolicySubscription) => s.status === 'ACTIVE').length || 0;
        pendingSubs = subs?.filter(isPending).length || 0;
        this.pendingSubscriptions.set(subs?.filter(isPending).slice(0, 5) || []);
        this.updateCards(customersCount, totalClaims, activePolicies, pendingSubs, totalPolicies, totalUnderwriters);
      },
      error: (err: Error) => {
        console.error('Error loading subscriptions:', err);
        this.errorMessage.set('Failed to load subscription data');
      }
    });

    this.adminService.getAllPolicies().subscribe({
      next: (policies) => {
        totalPolicies = policies?.length || 0;
        this.updateCards(customersCount, totalClaims, activePolicies, pendingSubs, totalPolicies, totalUnderwriters);
        this.isLoading.set(false);
      },
      error: () => { this.isLoading.set(false); }
    });
  }

  private updateCards(customers: number, claims: number, active: number, pending: number, policies: number, underwriters: number): void {
    this.dashboardCards.set([
      { title: 'Total Customers', value: customers, icon: 'people', color: '#C72B32', route: '/admin-dashboard/customers' },
      { title: 'Underwriters', value: underwriters, icon: 'manage_accounts', color: '#8b5cf6', route: '/admin-dashboard/underwriters' },
      { title: 'Total Policies', value: policies, icon: 'description', color: '#D41F59', route: '/admin-dashboard/policies' },
      { title: 'Active Subscriptions', value: active, icon: 'verified', color: '#10b981', route: '/admin-dashboard/subscriptions' },
      { title: 'Pending Approvals', value: pending, icon: 'pending_actions', color: '#f59e0b', route: '/admin-dashboard/subscriptions' },
      { title: 'Total Claims', value: claims, icon: 'assignment', color: '#C72B32', route: '/admin-dashboard/claims' }
    ]);
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' });
  }

  getStatusClass(status: string): string {
    const statusMap: { [key: string]: string } = {
      'SUBMITTED': 'bg-blue-100 text-blue-800',
      'INSPECTING': 'bg-yellow-100 text-yellow-800',
      'INSPECTED': 'bg-purple-100 text-purple-800',
      'APPROVED': 'bg-green-100 text-green-800',
      'REJECTED': 'bg-red-100 text-red-800',
      'PENDING': 'bg-yellow-100 text-yellow-800',
      'ACTIVE': 'bg-green-100 text-green-800',
      'REQUESTED': 'bg-blue-100 text-blue-800',
      'INSPECTION_PENDING': 'bg-orange-100 text-orange-800',
      'UNDER_REVIEW': 'bg-indigo-100 text-indigo-800',
      'PAYMENT_PENDING': 'bg-emerald-100 text-emerald-800'
    };
    return statusMap[status] || 'bg-gray-100 text-gray-800';
  }

  getStatusLabel(status: string): string {
    return status.split('_').map(word => word.charAt(0) + word.slice(1).toLowerCase()).join(' ');
  }

  getStatusColor(status: string): string {
    const colorMap: { [key: string]: string } = {
      'SUBMITTED': '#C72B32',
      'INSPECTING': '#FF6B35',
      'INSPECTED': '#E2725B',
      'APPROVED': '#10b981',
      'REJECTED': '#C72B32',
      'SETTLED': '#C72B32',
      'PENDING': '#FF6B35',
      'ACTIVE': '#10b981',
      'INSPECTION_PENDING': '#f59e0b',
      'UNDER_REVIEW': '#6366f1',
      'PAYMENT_PENDING': '#10b981',
      'REQUESTED': '#3b82f6'
    };
    return colorMap[status] || '#cbd5e1';
  }

  approveSubscription(id: number): void {
    this.adminService.approveSubscription(id).subscribe({
      next: () => this.loadDashboardData(),
      error: (err: Error) => {
        console.error('Error approving subscription:', err);
        this.errorMessage.set('Failed to approve subscription');
      }
    });
  }

  rejectSubscription(id: number): void {
    this.adminService.rejectSubscription(id).subscribe({
      next: () => this.loadDashboardData(),
      error: (err: Error) => {
        console.error('Error rejecting subscription:', err);
        this.errorMessage.set('Failed to reject subscription');
      }
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
}
