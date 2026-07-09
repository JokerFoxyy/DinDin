import { Component } from '@angular/core';

import { PagePlaceholder } from '../../shared/page-placeholder';

@Component({
  selector: 'app-settings',
  imports: [PagePlaceholder],
  template: '<app-page-placeholder title="Configurações" session="sessão #5" />'
})
export class Settings {}
