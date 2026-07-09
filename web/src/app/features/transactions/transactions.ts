import { Component } from '@angular/core';

import { PagePlaceholder } from '../../shared/page-placeholder';

@Component({
  selector: 'app-transactions',
  imports: [PagePlaceholder],
  template: '<app-page-placeholder title="Transações" session="sessão #7" />'
})
export class Transactions {}
