import { TestBed } from '@angular/core/testing';
import { Type } from '@angular/core';

import { Dashboard } from './dashboard/dashboard';
import { Transactions } from './transactions/transactions';
import { Investments } from './investments/investments';
import { Goals } from './goals/goals';
import { Recurring } from './recurring/recurring';

describe('Páginas placeholder', () => {
  const cases: { component: Type<unknown>; title: string }[] = [
    { component: Dashboard, title: 'Dashboard' },
    { component: Transactions, title: 'Transações' },
    { component: Investments, title: 'Investimentos' },
    { component: Goals, title: 'Metas' },
    { component: Recurring, title: 'Fixos' }
  ];

  for (const { component, title } of cases) {
    it(`should render the ${title} page title`, async () => {
      await TestBed.configureTestingModule({ imports: [component] }).compileComponents();
      const fixture = TestBed.createComponent(component);
      fixture.detectChanges();

      expect((fixture.nativeElement as HTMLElement).querySelector('h1')?.textContent).toContain(title);
    });
  }
});
