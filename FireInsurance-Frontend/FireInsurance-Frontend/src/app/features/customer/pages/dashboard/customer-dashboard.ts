import { Component, OnInit, inject, signal, WritableSignal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { CustomerService } from '../../services/customer.service';
import { Subscription } from '../../../../core/models/customer.model';
import { Claim } from '../../../../core/models/claim.model';

interface DashboardCard {
  title: string;
  value: number;
  icon: string;
  color: string;
  subtitle?: string;
}

@Component({
  selector: 'app-customer-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './customer-dashboard.html'
  // Styles provided via Tailwind utility classes
})
export class CustomerDashboardComponent implements OnInit {
  private readonly customerService = inject(CustomerService);
  private readonly router = inject(Router);

  dashboardCards: WritableSignal<DashboardCard[]> = signal([
    {
      title: 'Total Properties',
      value: 0,
      icon: 'home',
      color: '#C72B32'
    },
    {
      title: 'Active Policies',
      value: 0,
      icon: 'description',
      color: '#10b981'
    },
    {
      title: 'Claims Filed',
      value: 0,
      icon: 'assignment',
      color: '#FF6B35'
    },
    {
      title: 'Pending Claims',
      value: 0,
      icon: 'pending',
      color: '#FF6B35'
    },
    {
      title: 'Uploaded Documents',
      value: 0,
      icon: 'folder',
      color: '#E2725B'
    }
  ]);

  recentPolicies = signal<Subscription[]>([]);
  recentClaims = signal<Claim[]>([]);
  isLoading = signal<boolean>(false);

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.isLoading.set(true);

    this.customerService.getMyProperties().subscribe({
      next: (properties) => {
        this.dashboardCards.update(cards => {
          const updated = [...cards];
          updated[0].value = properties?.length || 0;
          return updated;
        });
      },
      error: (err) => console.error('Error loading properties:', err)
    });

    this.customerService.getMySubscriptions().subscribe({
      next: (subscriptions) => {
        const distinctSubscriptions = this.getLatestDistinctSubscriptions(subscriptions || []);
        const activeSubscriptions = distinctSubscriptions.filter(subscription => subscription.status === 'ACTIVE');

        this.dashboardCards.update(cards => {
          const updated = [...cards];
          updated[1].value = activeSubscriptions.length;
          return updated;
        });
        this.recentPolicies.set(activeSubscriptions.slice(0, 3));
      },
      error: (err) => console.error('Error loading subscriptions:', err)
    });

    this.customerService.getMyClaims().subscribe({
      next: (claims) => {
        // Apply smart calculation for settlement amounts
        const processedClaims = (claims || []).map(claim => this.calculateSettlementAmount(claim));
        const pendingClaims = processedClaims.filter(c => c.status === 'SUBMITTED' || c.status === 'INSPECTING');
        this.dashboardCards.update(cards => {
          const updated = [...cards];
          updated[2].value = processedClaims.length;
          updated[3].value = pendingClaims.length;
          return updated;
        });
        // Store recent 2 claims
        this.recentClaims.set(processedClaims.slice(0, 2));
      },
      error: (err) => console.error('Error loading claims:', err)
    });

    this.customerService.getMyDocuments().subscribe({
      next: (documents) => {
        this.dashboardCards.update(cards => {
          const updated = [...cards];
          updated[4].value = documents?.length || 0;
          return updated;
        });
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error loading documents:', err);
        this.isLoading.set(false);
      }
    });
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
  }

  getMonthlyPremium(subscription: Subscription): number | null {
    if (!subscription.premiumAmount || subscription.premiumAmount <= 0) {
      return null;
    }

    const months = this.getPolicyDurationMonths(subscription);
    return Math.round((subscription.premiumAmount / months) * 100) / 100;
  }

  private getPolicyDurationMonths(subscription: Subscription): number {
    if (!subscription.startDate || !subscription.endDate) {
      return 12;
    }

    const startDate = new Date(subscription.startDate);
    const endDate = new Date(subscription.endDate);
    if (Number.isNaN(startDate.getTime()) || Number.isNaN(endDate.getTime()) || endDate <= startDate) {
      return 12;
    }

    return Math.max(1, Math.round((endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24 * 30.44)));
  }

  private getLatestDistinctSubscriptions(subscriptions: Subscription[]): Subscription[] {
    const latestSubscriptions = new Map<string, Subscription>();

    [...subscriptions]
      .sort((left, right) => (right.subscriptionId || 0) - (left.subscriptionId || 0))
      .forEach(subscription => {
        const key = `${subscription.propertyId}-${subscription.policyName}`;
        if (!latestSubscriptions.has(key)) {
          latestSubscriptions.set(key, subscription);
        }
      });

    return Array.from(latestSubscriptions.values());
  }

  getStatusClass(status: string): string {
    const statusMap: { [key: string]: string } = {
      'ACTIVE': 'bg-green-100 text-green-800',
      'EXPIRED': 'bg-gray-100 text-gray-800',
      'CANCELLED': 'bg-red-100 text-red-800',
      'SUBMITTED': 'bg-blue-100 text-blue-800',
      'UNDER_REVIEW': 'bg-yellow-100 text-yellow-800',
      'INSPECTING': 'bg-yellow-100 text-yellow-800',
      'INSPECTED': 'bg-purple-100 text-purple-800',
      'APPROVED': 'bg-green-100 text-green-800',
      'REJECTED': 'bg-red-100 text-red-800',
      'SETTLED': 'bg-purple-100 text-purple-800',
      'PAID': 'bg-green-100 text-green-800'
    };
    return statusMap[status] || 'bg-gray-100 text-gray-800';
  }

  getStatusLabel(status: string): string {
    return status.split('_').map(word => word.charAt(0) + word.slice(1).toLowerCase()).join(' ');
  }

  navigateTo(route: string): void {
    this.router.navigate([`/customer/${route}`]);
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

