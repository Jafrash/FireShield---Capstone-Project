import { Routes } from '@angular/router';
import { roleGuard } from '../../core/guards';

export const siuInvestigatorRoutes: Routes = [
  {
    path: '',
    canActivate: [roleGuard],
    data: { roles: ['SIU_INVESTIGATOR'] },
    loadComponent: () => import('../../shared/components/layout/dashboard-layout/dashboard-layout').then(m => m.DashboardLayoutComponent),
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },
      {
        path: 'dashboard',
        loadComponent: () => import('./pages/dashboard/siu-investigator-dashboard').then(m => m.SiuInvestigatorDashboardComponent)
      },
      {
        path: 'profile',
        loadComponent: () => import('./pages/profile/siu-investigator-profile').then(m => m.SiuInvestigatorProfileComponent)
      },
      {
        path: 'cases',
        loadComponent: () => import('./pages/cases/assigned-cases').then(m => m.AssignedCasesComponent)
      },
      {
        path: 'case/:id',
        loadComponent: () => import('./pages/case-detail/case-detail.component').then(m => m.CaseDetailComponent)
      },
      {
        path: 'reports',
        loadComponent: () => import('./pages/reports/investigation-reports').then(m => m.InvestigationReportsComponent)
      }
    ]
  }
];