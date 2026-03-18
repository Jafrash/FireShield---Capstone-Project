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
  analyticsCards = signal<AnalyticsCard[]>([...ADVANCED_ANALYTICS]);

  // Raw data for analytics
  allClaims: Claim[] = [];
  allPolicies: any[] = [];
  allInspections: any[] = [];
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
        this.allClaims = claims || [];
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
    // Inspections
    this.adminService.getAllInspections().subscribe({
      next: (inspections) => {
        this.allInspections = inspections || [];
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
    const inspections = this.allInspections;
    const subs = this.allSubscriptions;

    // Claims Approval Rate
    const approvedClaims = claims.filter(c => c.status === 'APPROVED').length;
    const approvalRate = claims.length > 0 ? ((approvedClaims / claims.length) * 100).toFixed(1) + '%' : '--';

    // Average Claim Amount
    const avgClaimAmount = claims.length > 0 ?
      (claims.reduce((sum, c) => sum + (c.claimAmount || 0), 0) / claims.length).toLocaleString('en-IN', { maximumFractionDigits: 0 }) : '--';

    // Average Settlement Time (days between claimDate and updatedAt for APPROVED/SETTLED claims)
    const settledClaims = claims.filter(c => c.status === 'APPROVED' || c.status === 'SETTLED');
    let avgSettlementTime = '--';
    if (settledClaims.length > 0) {
      const totalDays = settledClaims.reduce((sum, c) => {
        const start = new Date(c.claimDate).getTime();
        const end = new Date(c.updatedAt).getTime();
        return sum + Math.max(0, Math.round((end - start) / (1000 * 60 * 60 * 24)));
      }, 0);
      avgSettlementTime = (totalDays / settledClaims.length).toFixed(1) + ' days';
    }

    // Active Policy Ratio
    const activePolicies = subs.filter(s => s.status === 'ACTIVE').length;
    const totalPolicies = subs.length;
    const activePolicyRatio = totalPolicies > 0 ? ((activePolicies / totalPolicies) * 100).toFixed(1) + '%' : '--';

    // Pending Inspections
    const pendingInspections = inspections.filter(i => i.status === 'PENDING' || i.status === 'ASSIGNED').length;

    // Update analytics cards
    this.analyticsCards.set([
      {
        title: 'Claims Approval Rate',
        value: approvalRate,
        icon: 'percent',
        color: '#3b82f6',
        description: 'Percentage of claims approved out of total claims.'
      },
      {
        title: 'Average Claim Amount',
        value: avgClaimAmount ? `₹${avgClaimAmount}` : '--',
        icon: 'payments',
        color: '#8b5cf6',
        description: 'Mean value of all claims submitted.'
      },
      {
        title: 'Average Settlement Time',
        value: avgSettlementTime,
        icon: 'schedule',
        color: '#10b981',
        description: 'Average days taken to settle a claim.'
      },
      {
        title: 'Active Policy Ratio',
        value: activePolicyRatio,
        icon: 'pie_chart',
        color: '#f59e0b',
        description: 'Ratio of active policies to total policies.'
      },
      {
        title: 'Pending Inspections',
        value: pendingInspections,
        icon: 'assignment_late',
        color: '#ef4444',
        description: 'Number of inspections pending completion.'
      }
    ]);
  }

  private loadDashboardData(): void {
    this.isLoading.set(true);

    // Load all data in parallel
    let customersCount = 0;
    let totalClaims = 0;
    let pendingInspections = 0;
    let activePolicies = 0;
    let pendingSubs = 0;
    let totalPolicies = 0;
    let totalUnderwriters = 0;

    this.adminService.getAllUnderwriters().subscribe({
      next: (uws) => {
        totalUnderwriters = uws?.length || 0;
        this.updateCards(customersCount, totalClaims, pendingInspections, activePolicies, pendingSubs, totalPolicies, totalUnderwriters);
      },
      error: () => {}
    });

    this.adminService.getAllCustomers().subscribe({
      next: (customers) => {
        customersCount = customers?.length || 0;
        this.updateCards(customersCount, totalClaims, pendingInspections, activePolicies, pendingSubs, totalPolicies, totalUnderwriters);
      },
      error: () => {}
    });

    this.adminService.getAllClaims().subscribe({
      next: (claims) => {
        totalClaims = claims?.length || 0;
        this.recentClaims.set((claims || []).slice(0, 5));
        this.updateCards(customersCount, totalClaims, pendingInspections, activePolicies, pendingSubs, totalPolicies, totalUnderwriters);
      },
      error: () => {}
    });

    this.adminService.getAllSubscriptions().subscribe({
      next: (subs) => {
        activePolicies = subs?.filter((s: PolicySubscription) => s.status === 'ACTIVE').length || 0;
        pendingSubs = subs?.filter((s: PolicySubscription) => s.status === 'PENDING').length || 0;
        this.pendingSubscriptions.set(subs?.filter((s: PolicySubscription) => s.status === 'PENDING').slice(0, 5) || []);
        this.updateCards(customersCount, totalClaims, pendingInspections, activePolicies, pendingSubs, totalPolicies, totalUnderwriters);
      },
      error: (err: Error) => {
        console.error('Error loading subscriptions:', err);
        this.errorMessage.set('Failed to load subscription data');
      }
    });

    this.adminService.getAllPolicies().subscribe({
      next: (policies) => {
        totalPolicies = policies?.length || 0;
        this.updateCards(customersCount, totalClaims, pendingInspections, activePolicies, pendingSubs, totalPolicies, totalUnderwriters);
        this.isLoading.set(false);
      },
      error: () => { this.isLoading.set(false); }
    });
  }

  private updateCards(customers: number, claims: number, inspections: number, active: number, pending: number, policies: number, underwriters: number): void {
    this.dashboardCards.set([
      { title: 'Total Customers', value: customers, icon: 'people', color: '#8B1538', route: '/admin/customers' },
      { title: 'Underwriters', value: underwriters, icon: 'manage_accounts', color: '#8b5cf6', route: '/admin/underwriters' },
      { title: 'Total Policies', value: policies, icon: 'description', color: '#D41F59', route: '/admin/policies' },
      { title: 'Active Subscriptions', value: active, icon: 'verified', color: '#10b981', route: '/admin/subscriptions' },
      { title: 'Pending Approvals', value: pending, icon: 'pending_actions', color: '#f59e0b', route: '/admin/subscriptions' },
      { title: 'Total Claims', value: claims, icon: 'assignment', color: '#8B1538', route: '/admin/claims' }
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
      'REQUESTED': 'bg-blue-100 text-blue-800'
    };
    return statusMap[status] || 'bg-gray-100 text-gray-800';
  }

  getStatusLabel(status: string): string {
    return status.split('_').map(word => word.charAt(0) + word.slice(1).toLowerCase()).join(' ');
  }

  getStatusColor(status: string): string {
    const colorMap: { [key: string]: string } = {
      'SUBMITTED': '#3b82f6',
      'INSPECTING': '#f59e0b',
      'INSPECTED': '#8b5cf6',
      'APPROVED': '#10b981',
      'REJECTED': '#ef4444',
      'SETTLED': '#8B1538',
      'PENDING': '#f59e0b',
      'ACTIVE': '#10b981'
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
}
