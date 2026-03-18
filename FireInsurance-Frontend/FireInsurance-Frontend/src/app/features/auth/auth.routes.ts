import { Routes } from '@angular/router';
import { loggedInGuard } from '../../core/guards';

export const authRoutes: Routes = [
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },
  {
    path: 'login',
    canActivate: [loggedInGuard],
    loadComponent: () => import('./pages/login/login').then(m => m.LoginComponent)
  },
  {
    path: 'forgot-password',
    canActivate: [loggedInGuard],
    loadComponent: () => import('./pages/forgot-password/forgot-password').then(m => m.ForgotPasswordComponent)
  },
  {
    path: 'register',
    canActivate: [loggedInGuard],
    loadComponent: () => import('./pages/register/register').then(m => m.RegisterComponent)
  },
  {
    path: 'unauthorized',
    loadComponent: () => import('./pages/unauthorized/unauthorized').then(m => m.UnauthorizedComponent)
  }
];
