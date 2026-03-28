import { Routes } from '@angular/router';
import { roleGuard } from '../../core/guards';
import { DashboardLayoutComponent } from '../../shared/components/layout/dashboard-layout/dashboard-layout';

export const underwriterRoutes: Routes = [
  {
    path: '',
    component: DashboardLayoutComponent,
    canActivate: [roleGuard],
    data: { roles: ['UNDERWRITER'] },
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./pages/dashboard/uw-dashboard.component').then(m => m.UwDashboardComponent)
      },
      {
        path: 'subscriptions',
        loadComponent: () =>
          import('./pages/subscriptions/uw-subscriptions.component').then(m => m.UwSubscriptionsComponent)
      },
      {
        path: 'claims',
        loadComponent: () =>
          import('./pages/claims/uw-claims.component').then(m => m.UwClaimsComponent)
      },
      {
        path: 'property-inspections',
        loadComponent: () =>
          import('./pages/property-inspections/uw-property-inspections').then(m => m.UwPropertyInspectionsComponent)
      },
      {
        path: 'claim-inspections',
        loadComponent: () =>
          import('./pages/claim-inspections/uw-claim-inspections').then(m => m.UwClaimInspectionsComponent)
      }
    ]
  }
];
