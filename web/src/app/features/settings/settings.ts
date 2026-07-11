import { Component } from '@angular/core';

import { AccountsPanel } from './accounts-panel';
import { CategoriesPanel } from './categories-panel';

@Component({
  selector: 'app-settings',
  imports: [AccountsPanel, CategoriesPanel],
  template: `
    <div class="topbar">
      <h1>Configurações</h1>
    </div>
    <div class="grid2">
      <app-accounts-panel />
      <app-categories-panel />
    </div>
  `,
  styleUrl: './settings.css'
})
export class Settings {}
