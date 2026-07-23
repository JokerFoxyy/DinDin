import { Component } from '@angular/core';

import { AccountsPanel } from './accounts-panel';
import { CardsPanel } from './cards-panel';
import { CategoriesPanel } from './categories-panel';
import { PrivacyPanel } from './privacy-panel';

@Component({
  selector: 'app-settings',
  imports: [AccountsPanel, CardsPanel, CategoriesPanel, PrivacyPanel],
  template: `
    <div class="topbar">
      <h1>Configurações</h1>
    </div>
    <div class="grid2">
      <app-accounts-panel />
      <app-cards-panel />
      <app-categories-panel />
    </div>
    <div class="privacy-row">
      <app-privacy-panel />
    </div>
  `,
  styleUrl: './settings.css'
})
export class Settings {}
