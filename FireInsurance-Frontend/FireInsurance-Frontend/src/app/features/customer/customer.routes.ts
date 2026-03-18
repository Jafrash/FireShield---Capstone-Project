import { Routes } from '@angular/router';
import { roleGuard } from '../../core/guards';

export const customerRoutes: Routes = [
  {
    path: '',
    canActivate: [roleGuard],
    data: { roles: ['CUSTOMER'] },
    loadComponent: () => import('../../shared/components/layout/dashboard-layout/dashboard-layout').then(m => m.DashboardLayoutComponent),
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },
      {
        path: 'dashboard',
        loadComponent: () => import('./pages/dashboard/customer-dashboard').then(m => m.CustomerDashboardComponent)
      },
      {
        path: 'profile',
        loadComponent: () => import('./pages/profile/profile').then(m => m.ProfileComponent)
      },
      {
        path: 'properties',
        loadComponent: () => import('./pages/properties/properties').then(m => m.PropertiesComponent)
      },
      {
        path: 'subscriptions',
        loadComponent: () => import('./pages/subscriptions/subscriptions').then(m => m.SubscriptionsComponent)
      },
      {
        path: 'policies',
        loadComponent: () => import('./pages/policies/customer-policies').then(m => m.CustomerPoliciesComponent)
      },
      {
        path: 'policies/:id',
        loadComponent: () => import('./pages/policy-details/policy-details').then(m => m.PolicyDetailsComponent)
      },
      {
        path: 'claims',
        loadComponent: () => import('./pages/claims/claims').then(m => m.CustomerClaimsComponent)
      }
    ]
  }
];
