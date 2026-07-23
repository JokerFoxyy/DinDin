import { Injectable, signal } from '@angular/core';

export type Theme = 'light' | 'dark';

const STORAGE_KEY = 'poupito.theme';

/**
 * Tema claro é o padrão da marca; o usuário pode alternar para o escuro.
 * A escolha persiste em localStorage; sem escolha salva, respeita a
 * preferência do sistema (prefers-color-scheme). O atributo data-theme no
 * <html> é o que os overrides de CSS observam.
 *
 * O tema inicial já é aplicado por um script inline no index.html (evita
 * "flash" de cor errada antes do Angular subir); este serviço apenas
 * sincroniza o signal com o que já está no DOM e trata a alternância.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly theme = signal<Theme>(this.currentFromDom());

  toggle(): void {
    this.set(this.theme() === 'dark' ? 'light' : 'dark');
  }

  set(theme: Theme): void {
    this.theme.set(theme);
    document.documentElement.setAttribute('data-theme', theme);
    try {
      localStorage.setItem(STORAGE_KEY, theme);
    } catch {
      /* localStorage indisponível (modo privado) — só não persiste */
    }
  }

  private currentFromDom(): Theme {
    return document.documentElement.getAttribute('data-theme') === 'dark' ? 'dark' : 'light';
  }
}
