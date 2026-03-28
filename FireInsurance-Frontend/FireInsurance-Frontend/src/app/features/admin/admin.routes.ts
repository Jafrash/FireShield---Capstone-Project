import { Routes } from '@angular/router';
import { roleGuard } from '../../core/guards';
import { DashboardLayoutComponent } from '../../shared/components/layout/dashboard-layout/dashboard-layout';

export const adminRoutes: Routes = [
  {
    path: '',
    component: DashboardLayoutComponent,
    canActivate: [roleGuard],
    data: { roles: ['ADMIN'] },
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },
      {
        path: 'dashboard',
        loadComponent: () => import('./pages/dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent)
      },
      {
        path: 'customers',
        loadComponent: () => import('./pages/customers/customers').then(m => m.CustomersComponent)
      },
      {
        path: 'surveyors',
        loadComponent: () => import('./pages/surveyors/surveyors').then(m => m.SurveyorsComponent)
      },
      {
        path: 'policies',
        loadComponent: () => import('./pages/policies/policies').then(m => m.PoliciesComponent)
      },
      {
        path: 'claims',
        loadComponent: () => import('./pages/claims/claims').then(m => m.ClaimsComponent)
      },
      {
        path: 'subscriptions',
        loadComponent: () => import('./pages/subscriptions/subscriptions').then(m => m.AdminSubscriptionsComponent)
      },
      {
        path: 'underwriters',
        loadComponent: () => import('./pages/underwriters/underwriters').then(m => m.UnderwritersComponent)
      },
      {
        path: 'siu-investigators',
        loadComponent: () => import('./pages/siu-investigators/siu-investigators').then(m => m.SiuInvestigatorsComponent)
      },
      {
        path: 'blacklist',
        loadComponent: () => import('./pages/blacklist/blacklist').then(m => m.BlacklistComponent)
      },
      {
        path: 'fraud-monitoring',
        loadComponent: () => import('./pages/fraud-monitoring/fraud-monitoring.component').then(m => m.FraudMonitoringComponent)
      }
    ]
  }
];
