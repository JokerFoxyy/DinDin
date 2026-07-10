import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';
import { Shell } from './core/layout/shell';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login').then((m) => m.Login)
  },
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard)
      },
      {
        path: 'transacoes',
        loadComponent: () => import('./features/transactions/transactions').then((m) => m.Transactions)
      },
      {
        path: 'investimentos',
        loadComponent: () => import('./features/investments/investments').then((m) => m.Investments)
      },
      {
        path: 'metas',
        loadComponent: () => import('./features/goals/goals').then((m) => m.Goals)
      },
      {
        path: 'fixos',
        loadComponent: () => import('./features/recurring/recurring').then((m) => m.Recurring)
      },
      {
        path: 'configuracoes',
        loadComponent: () => import('./features/settings/settings').then((m) => m.Settings)
      }
    ]
  },
  { path: '**', redirectTo: '' }
];
