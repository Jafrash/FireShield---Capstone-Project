import { Routes } from '@angular/router';
import { roleGuard } from '../../core/guards';

export const siuRoutes: Routes = [
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
        loadComponent: () => import('./pages/dashboard/siu-dashboard.component').then(m => m.SiuDashboardComponent)
      },
      {
        path: 'claim/:id',
        loadComponent: () => import('./pages/claim-details/siu-claim-details.component').then(m => m.SiuClaimDetailsComponent)
      }
    ]
  }
];