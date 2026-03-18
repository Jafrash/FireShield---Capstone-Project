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
        path: 'property-inspections',
        loadComponent: () => import('./pages/property-inspections/property-inspections').then(m => m.PropertyInspectionsComponent)
      },
      {
        path: 'claim-inspections',
        loadComponent: () => import('./pages/claim-inspections/claim-inspections').then(m => m.ClaimInspectionsComponent)
      },
      {
        path: 'underwriters',
        loadComponent: () => import('./pages/underwriters/underwriters').then(m => m.UnderwritersComponent)
      }
    ]
  }
];
