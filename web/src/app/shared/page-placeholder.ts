import { Component, input } from '@angular/core';

@Component({
  selector: 'app-page-placeholder',
  template: `
    <div class="topbar">
      <h1>{{ title() }}</h1>
    </div>
    <div class="panel">
      <p class="placeholder-text">Em construção — chega na {{ session() }}.</p>
    </div>
  `,
  styles: `
    .topbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
    h1 { font-size: 22px; }
    .placeholder-text { color: var(--muted); font-size: 14px; }
  `
})
export class PagePlaceholder {
  readonly title = input.required<string>();
  readonly session = input.required<string>();
}
