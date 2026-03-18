import { Routes } from '@angular/router';
import { roleGuard } from '../../core/guards';

export const surveyorRoutes: Routes = [
  {
    path: '',
    canActivate: [roleGuard],
    data: { roles: ['SURVEYOR'] },
    loadComponent: () => import('../../shared/components/layout/dashboard-layout/dashboard-layout').then(m => m.DashboardLayoutComponent),
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },
      {
        path: 'dashboard',
        loadComponent: () => import('./pages/dashboard/surveyor-dashboard').then(m => m.SurveyorDashboardComponent)
      },
      {
        path: 'profile',
        loadComponent: () => import('./pages/profile/surveyor-profile').then(m => m.SurveyorProfileComponent)
      },
      {
        path: 'property-inspections',
        loadComponent: () => import('./pages/property-inspections/property-inspections').then(m => m.PropertyInspectionsComponent)
      },
      {
        path: 'claim-inspections',
        loadComponent: () => import('./pages/claim-inspections/claim-inspections').then(m => m.ClaimInspectionsComponent)
      }
    ]
  }
];
