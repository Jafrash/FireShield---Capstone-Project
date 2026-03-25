import { Routes } from '@angular/router';
import { authGuard } from './core/guards';

export const routes: Routes = [
  {
    path: '',
    loadChildren: () => import('./features/public/public.routes').then(m => m.publicRoutes)
  },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then(m => m.authRoutes)
  },
  {
    path: 'admin',
    canActivate: [authGuard],
    loadChildren: () => import('./features/admin/admin.routes').then(m => m.adminRoutes)
  },
  {
    path: 'customer',
    canActivate: [authGuard],
    loadChildren: () => import('./features/customer/customer.routes').then(m => m.customerRoutes)
  },
  {
    path: 'surveyor',
    canActivate: [authGuard],
    loadChildren: () => import('./features/surveyor/surveyor.routes').then(m => m.surveyorRoutes)
  },
  {
    path: 'underwriter',
    canActivate: [authGuard],
    loadChildren: () => import('./features/underwriter/underwriter.routes').then(m => m.underwriterRoutes)
  },
  {
    path: 'siu-investigator',
    canActivate: [authGuard],
    loadChildren: () => import('./features/siu-investigator/siu-investigator.routes').then(m => m.siuInvestigatorRoutes)
  },
  {
    path: '**',
    redirectTo: '/'
  }
];
