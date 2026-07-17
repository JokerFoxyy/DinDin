import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../auth/auth.service';
import { BudgetService } from '../../features/budgets/budget.service';

interface NavItem {
  icon: string;
  label: string;
  path: string;
}

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.html',
  styleUrl: './shell.css'
})
export class Shell implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly budgetService = inject(BudgetService);
  private readonly router = inject(Router);

  readonly currentUser = this.authService.currentUser;
  readonly budgetAlertCount = signal(0);
  readonly sidebarOpen = signal(false);

  readonly navItems: NavItem[] = [
    { icon: '📊', label: 'Dashboard', path: '/dashboard' },
    { icon: '💸', label: 'Transações', path: '/transacoes' },
    { icon: '💳', label: 'Faturas', path: '/faturas' },
    { icon: '📈', label: 'Investimentos', path: '/investimentos' },
    { icon: '🎯', label: 'Metas', path: '/metas' },
    { icon: '🔁', label: 'Fixos', path: '/fixos' },
    { icon: '📋', label: 'Orçamentos', path: '/orcamentos' },
    { icon: '📥', label: 'Importar', path: '/importar' },
    { icon: '⚙️', label: 'Configurações', path: '/configuracoes' }
  ];

  ngOnInit(): void {
    this.authService.loadCurrentUser().subscribe({
      next: () => this.budgetService.alerts().subscribe((alerts) => this.budgetAlertCount.set(alerts.length)),
      error: () => {
        this.authService.clearSession();
        this.router.navigate(['/login']);
      }
    });
  }

  toggleSidebar(): void {
    this.sidebarOpen.update((open) => !open);
  }

  closeSidebar(): void {
    this.sidebarOpen.set(false);
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => this.router.navigate(['/login'])
    });
  }
}
