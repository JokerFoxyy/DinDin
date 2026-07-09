import { Component } from '@angular/core';

import { PagePlaceholder } from '../../shared/page-placeholder';

@Component({
  selector: 'app-dashboard',
  imports: [PagePlaceholder],
  template: '<app-page-placeholder title="Dashboard" session="sessão #11" />'
})
export class Dashboard {}
