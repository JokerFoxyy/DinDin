import { Component, computed, model } from '@angular/core';

/** Seletor de mês no formato 'YYYY-MM', com label pt-BR (ex.: "Julho 2026"). */
@Component({
  selector: 'app-month-picker',
  template: `
    <div class="month-picker">
      <button type="button" aria-label="Mês anterior" (click)="shift(-1)">‹</button>
      <span>{{ label() }}</span>
      <button type="button" aria-label="Próximo mês" (click)="shift(1)">›</button>
    </div>
  `,
  styles: `
    .month-picker {
      display: flex;
      align-items: center;
      gap: 8px;
      background: var(--card);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 6px 12px;
    }
    button {
      background: none;
      border: none;
      color: var(--accent);
      font-size: 16px;
      cursor: pointer;
      padding: 2px 6px;
    }
    span {
      font-size: 14px;
      font-weight: 600;
      min-width: 110px;
      text-align: center;
    }
  `
})
export class MonthPicker {
  readonly month = model.required<string>();

  readonly label = computed(() => {
    const [year, month] = this.month().split('-').map(Number);
    const name = new Intl.DateTimeFormat('pt-BR', { month: 'long' }).format(new Date(year, month - 1, 1));
    return `${name.charAt(0).toUpperCase()}${name.slice(1)} ${year}`;
  });

  shift(delta: number): void {
    const [year, month] = this.month().split('-').map(Number);
    const shifted = new Date(year, month - 1 + delta, 1);
    this.month.set(`${shifted.getFullYear()}-${String(shifted.getMonth() + 1).padStart(2, '0')}`);
  }
}
