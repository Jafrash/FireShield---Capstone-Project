import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SurveyorService, SurveyorDashboardStats } from '../../services/surveyor.service';
import { PropertyInspection, ClaimInspectionItem } from '../../../../core/models/inspection.model';

interface DashboardCard {
  title: string;
  value: number;
  icon: string;
  color: string;
  route?: string;
}

@Component({
  selector: 'app-surveyor-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './surveyor-dashboard.html',
  styleUrls: ['./surveyor-dashboard.css']
})
export class SurveyorDashboardComponent implements OnInit {
  private surveyorService = inject(SurveyorService);

  dashboardCards = signal<DashboardCard[]>([]);
  recentPropertyInspections = signal<PropertyInspection[]>([]);
  recentClaimInspections = signal<ClaimInspectionItem[]>([]);
  isLoading = signal<boolean>(true);
  errorMessage = signal<string>('');

  ngOnInit(): void {
    this.loadDashboardData();
  }

  private loadDashboardData(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.surveyorService.getDashboardStats().subscribe({
      next: (stats: SurveyorDashboardStats) => {
        this.dashboardCards.set([
          {
            title: 'Assigned Property Inspections',
            value: stats.assignedPropertyInspections || 0,
            icon: 'home_work',
            color: '#C72B32',
            route: '/surveyor/property-inspections'
          },
          {
            title: 'Assigned Claim Inspections',
            value: stats.assignedClaimInspections || 0,
            icon: 'assignment',
            color: '#FF6B35',
            route: '/surveyor/claim-inspections'
          },
          {
            title: 'Completed Inspections',
            value: stats.completedInspections || 0,
            icon: 'check_circle',
            color: '#10b981'
          },
          {
            title: 'Pending Inspections',
            value: stats.pendingInspections || 0,
            icon: 'pending_actions',
            color: '#C72B32'
          }
        ]);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading dashboard stats:', error);
        this.errorMessage.set('Failed to load dashboard statistics');
        this.isLoading.set(false);
        this.dashboardCards.set([
          { title: 'Assigned Property Inspections', value: 0, icon: 'home_work', color: '#3b82f6', route: '/surveyor/property-inspections' },
          { title: 'Assigned Claim Inspections', value: 0, icon: 'assignment', color: '#f59e0b', route: '/surveyor/claim-inspections' },
          { title: 'Completed Inspections', value: 0, icon: 'check_circle', color: '#10b981' },
          { title: 'Pending Inspections', value: 0, icon: 'pending_actions', color: '#ef4444' }
        ]);
      }
    });

    // Load recent items
    this.surveyorService.getMyPropertyInspections().subscribe({
      next: (data) => this.recentPropertyInspections.set(data.slice(0, 3)),
      error: () => {}
    });

    this.surveyorService.getMyClaimInspections().subscribe({
      next: (data) => this.recentClaimInspections.set(data.slice(0, 3)),
      error: () => {}
    });
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      'ASSIGNED': 'bg-blue-100 text-blue-800',
      'UNDER_REVIEW': 'bg-yellow-100 text-yellow-800',
      'COMPLETED': 'bg-green-100 text-green-800',
      'APPROVED': 'bg-green-100 text-green-800',
      'REJECTED': 'bg-red-100 text-red-800'
    };
    return map[status] || 'bg-gray-100 text-gray-800';
  }
}
