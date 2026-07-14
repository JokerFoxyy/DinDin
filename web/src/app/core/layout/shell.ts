import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '../auth/auth.service';

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
  private readonly router = inject(Router);

  readonly currentUser = this.authService.currentUser;

  readonly navItems: NavItem[] = [
    { icon: '📊', label: 'Dashboard', path: '/dashboard' },
    { icon: '💸', label: 'Transações', path: '/transacoes' },
    { icon: '💳', label: 'Faturas', path: '/faturas' },
    { icon: '📈', label: 'Investimentos', path: '/investimentos' },
    { icon: '🎯', label: 'Metas', path: '/metas' },
    { icon: '🔁', label: 'Fixos', path: '/fixos' },
    { icon: '⚙️', label: 'Configurações', path: '/configuracoes' }
  ];

  ngOnInit(): void {
    this.authService.loadCurrentUser().subscribe({
      error: () => {
        this.authService.clearSession();
        this.router.navigate(['/login']);
      }
    });
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => this.router.navigate(['/login'])
    });
  }
}
