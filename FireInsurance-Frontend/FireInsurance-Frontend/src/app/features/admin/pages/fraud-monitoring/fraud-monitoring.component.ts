import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import {
  AdminService,
  FraudStatistics,
  FraudDistribution,
  SiuWorkload,
  FraudTrend
} from '../../services/admin.service';

@Component({
  selector: 'app-fraud-monitoring',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './fraud-monitoring.component.html'
})
export class FraudMonitoringComponent implements OnInit {
  private adminService = inject(AdminService);
  private router = inject(Router);

  // Statistics Data
  fraudStats = signal<FraudStatistics | null>(null);
  fraudDistribution = signal<FraudDistribution[]>([]);
  siuWorkload = signal<SiuWorkload[]>([]);
  fraudTrends = signal<FraudTrend[]>([]);

  // Loading States
  isStatsLoading = signal<boolean>(true);
  isDistributionLoading = signal<boolean>(true);
  isWorkloadLoading = signal<boolean>(true);
  isTrendsLoading = signal<boolean>(true);

  // Error Messages
  statsError = signal<string>('');
  distributionError = signal<string>('');
  workloadError = signal<string>('');
  trendsError = signal<string>('');

  ngOnInit(): void {
    this.loadAllFraudData();
  }

  loadAllFraudData(): void {
    this.loadFraudStatistics();
    this.loadFraudDistribution();
    this.loadSiuWorkload();
    this.loadFraudTrends();
  }

  loadFraudStatistics(): void {
    this.isStatsLoading.set(true);
    this.statsError.set('');

    this.adminService.getFraudStatistics().subscribe({
      next: (stats) => {
        console.log('[FRAUD-STATS] Received:', stats);
        this.fraudStats.set(stats);
        this.isStatsLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading fraud statistics:', error);
        this.statsError.set('Failed to load real-time fraud statistics. Please ensure the backend is running with the latest updates.');
        this.isStatsLoading.set(false);
      }
    });
  }

  loadFraudDistribution(): void {
    this.isDistributionLoading.set(true);
    this.distributionError.set('');

    this.adminService.getFraudDistribution().subscribe({
      next: (distribution) => {
        this.fraudDistribution.set(distribution);
        this.isDistributionLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading fraud distribution:', error);
        this.distributionError.set('Failed to load fraud distribution');
        this.isDistributionLoading.set(false);
      }
    });
  }

  loadSiuWorkload(): void {
    this.isWorkloadLoading.set(true);
    this.workloadError.set('');

    this.adminService.getSiuWorkload().subscribe({
      next: (workload) => {
        this.siuWorkload.set(workload);
        this.isWorkloadLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading SIU workload:', error);
        this.workloadError.set('Failed to load SIU workload');
        this.isWorkloadLoading.set(false);
      }
    });
  }

  loadFraudTrends(): void {
    this.isTrendsLoading.set(true);
    this.trendsError.set('');

    this.adminService.getFraudTrends().subscribe({
      next: (trends) => {
        this.fraudTrends.set(trends);
        this.isTrendsLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading fraud trends:', error);
        this.trendsError.set('Failed to load fraud trends');
        this.isTrendsLoading.set(false);
      }
    });
  }

  refreshData(): void {
    this.loadAllFraudData();
  }

  navigateToClaimsWithFilter(filter: 'high-risk' | 'medium-risk' | 'low-risk' | 'all'): void {
    // Navigate to claims page with fraud score filter
    this.router.navigate(['/admin-dashboard/claims'], {
      queryParams: { fraudFilter: filter }
    });
  }

  navigateToSiuDashboard(): void {
    this.router.navigate(['/siu-dashboard']);
  }

  getRiskLevelColor(riskLevel: string): string {
    switch (riskLevel) {
      case 'HIGH': return 'text-red-600 bg-red-50 border-red-200';
      case 'MEDIUM': return 'text-yellow-600 bg-yellow-50 border-yellow-200';
      case 'LOW': return 'text-green-600 bg-green-50 border-green-200';
      default: return 'text-gray-600 bg-gray-50 border-gray-200';
    }
  }

  getRiskLevelIcon(riskLevel: string): string {
    switch (riskLevel) {
      case 'HIGH': return 'warning';
      case 'MEDIUM': return 'info';
      case 'LOW': return 'check_circle';
      default: return 'help';
    }
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(value);
  }

  formatPercentage(value: number): string {
    return `${value.toFixed(1)}%`;
  }

  getCurrentTime(): string {
    return new Date().toLocaleString();
  }
}