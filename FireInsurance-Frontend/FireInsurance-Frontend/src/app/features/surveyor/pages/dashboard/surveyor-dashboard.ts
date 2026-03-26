import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { SurveyorService, SurveyorDashboardStats } from '../../services/surveyor.service';
import { PropertyInspection, ClaimInspectionItem } from '../../../../core/models/inspection.model';

import { AdminService, Surveyor } from '../../../admin/services/admin.service';

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
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './surveyor-dashboard.html',
  styleUrls: ['./surveyor-dashboard.css']
})
export class SurveyorDashboardComponent implements OnInit {
  private surveyorService = inject(SurveyorService);

  private adminService = inject(AdminService);

  dashboardCards = signal<DashboardCard[]>([]);
  recentPropertyInspections = signal<PropertyInspection[]>([]);
  recentClaimInspections = signal<ClaimInspectionItem[]>([]);
  isLoading = signal<boolean>(true);
  errorMessage = signal<string>('');

  unassignedPropertyInspections = signal<PropertyInspection[]>([]);
  availableSurveyors = signal<Surveyor[]>([]);
  selectedSurveyors: Record<number, number> = {};
  assignMessage = signal<string>('');

  ngOnInit(): void {
    this.loadDashboardData();
    this.loadUnassignedPropertyInspections();
    this.loadAvailableSurveyors();
  }

  private loadUnassignedPropertyInspections(): void {
    // Fetch all property inspections and filter those with no surveyor assigned or status 'PENDING'/'UNASSIGNED'
    this.surveyorService["http"].get<any[]>(`${this.surveyorService["apiUrl"]}/inspections`).subscribe({
      next: (data) => {
        const unassigned = (data || [])
          .filter((insp: any) => !insp.surveyor || !insp.surveyor.surveyorId || insp.status === 'PENDING' || insp.status === 'UNASSIGNED')
          .map((item: any) => this.surveyorService["normalizePropertyInspection"](item));
        this.unassignedPropertyInspections.set(unassigned);
      },
      error: () => {
        this.unassignedPropertyInspections.set([]);
      }
    });
  }

  private loadAvailableSurveyors(): void {
    this.adminService.getAllSurveyors().subscribe({
      next: (data) => this.availableSurveyors.set(data || []),
      error: () => this.availableSurveyors.set([])
    });
  }

  assignSurveyorToProperty(propertyId: number): void {
    const surveyorId = this.selectedSurveyors[propertyId];
    if (!surveyorId) {
      this.assignMessage.set('Please select a surveyor.');
      return;
    }
    this.surveyorService["http"].post(`${this.surveyorService["apiUrl"]}/inspections/assign/${propertyId}?surveyorId=${surveyorId}`, {}).subscribe({
      next: () => {
        this.assignMessage.set('Surveyor assigned successfully!');
        this.loadUnassignedPropertyInspections();
        setTimeout(() => this.assignMessage.set(''), 2000);
      },
      error: () => {
        this.assignMessage.set('Failed to assign surveyor.');
        setTimeout(() => this.assignMessage.set(''), 2000);
      }
    });
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
