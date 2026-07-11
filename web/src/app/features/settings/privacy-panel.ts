import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { PrivacyService } from './privacy.service';

const CONFIRM_WORD = 'EXCLUIR';

@Component({
  selector: 'app-privacy-panel',
  templateUrl: './privacy-panel.html',
  styleUrl: './privacy-panel.css'
})
export class PrivacyPanel {
  private readonly privacyService = inject(PrivacyService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly confirmText = signal('');
  readonly deleting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  canDelete(): boolean {
    return this.confirmText() === CONFIRM_WORD && !this.deleting();
  }

  onConfirmInput(value: string): void {
    this.confirmText.set(value);
  }

  exportData(): void {
    this.errorMessage.set(null);
    this.privacyService.exportData().subscribe({
      next: (blob) => this.download(blob),
      error: () => this.errorMessage.set('Erro ao exportar os dados')
    });
  }

  deleteAccount(): void {
    if (!this.canDelete()) {
      return;
    }
    this.deleting.set(true);
    this.errorMessage.set(null);
    this.privacyService.deleteAccount().subscribe({
      next: () => {
        this.authService.clearSession();
        this.router.navigate(['/login']);
      },
      error: () => {
        this.deleting.set(false);
        this.errorMessage.set('Erro ao excluir a conta');
      }
    });
  }

  private download(blob: Blob): void {
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = 'dindin-meus-dados.json';
    anchor.click();
    URL.revokeObjectURL(url);
  }
}
