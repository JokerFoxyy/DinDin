import { Component } from '@angular/core';

import { PagePlaceholder } from '../../shared/page-placeholder';

@Component({
  selector: 'app-goals',
  imports: [PagePlaceholder],
  template: '<app-page-placeholder title="Metas" session="sessão #16" />'
})
export class Goals {}
