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

  pendingInspections = computed(() =>
    this.subscriptions().filter(s =>
      s.status === 'REQUESTED' || s.status === 'PENDING' || s.status === 'INSPECTING'
    ).length
  );

  pendingDecisions = computed(() => {
    const pendingSubs = this.subscriptions().filter(s => s.status === 'INSPECTED').length;
    const pendingClaims = this.claims().filter(c => c.status === 'INSPECTED').length;
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
        color: '#8B1E3F',
        route: '/underwriter/subscriptions'
      },
      {
        title: 'Assigned Claims',
        value: this.assignedClaims(),
        icon: 'assignment',
        color: '#D81B60',
        route: '/underwriter/claims'
      },
      {
        title: 'Pending Inspections',
        value: this.pendingInspections(),
        icon: 'search',
        color: '#7B5EA7',
        route: '/underwriter/subscriptions'
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
