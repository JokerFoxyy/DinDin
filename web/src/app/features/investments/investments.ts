import { Component } from '@angular/core';

import { PagePlaceholder } from '../../shared/page-placeholder';

@Component({
  selector: 'app-investments',
  imports: [PagePlaceholder],
  template: '<app-page-placeholder title="Investimentos" session="sessão #15" />'
})
export class Investments {}
