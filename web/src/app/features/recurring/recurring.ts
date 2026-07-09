import { Component } from '@angular/core';

import { PagePlaceholder } from '../../shared/page-placeholder';

@Component({
  selector: 'app-recurring',
  imports: [PagePlaceholder],
  template: '<app-page-placeholder title="Fixos" session="sessão #8" />'
})
export class Recurring {}
