import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/guards';

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
    path: 'admin-dashboard',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadChildren: () => import('./features/admin/admin.routes').then(m => m.adminRoutes)
  },
  {
    path: 'siu-dashboard',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['SIU_INVESTIGATOR'] },
    loadChildren: () => import('./features/siu/siu.routes').then(m => m.siuRoutes)
  },
  {
    path: 'underwriter-dashboard',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['UNDERWRITER'] },
    loadChildren: () => import('./features/underwriter/underwriter.routes').then(m => m.underwriterRoutes)
  },
  {
    path: 'customer',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['CUSTOMER'] },
    loadChildren: () => import('./features/customer/customer.routes').then(m => m.customerRoutes)
  },
  {
    path: 'surveyor',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['SURVEYOR'] },
    loadChildren: () => import('./features/surveyor/surveyor.routes').then(m => m.surveyorRoutes)
  },
  // Legacy routes for backward compatibility
  {
    path: 'admin',
    redirectTo: 'admin-dashboard',
    pathMatch: 'full'
  },
  {
    path: 'underwriter',
    redirectTo: 'underwriter-dashboard',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: '/'
  }
];
