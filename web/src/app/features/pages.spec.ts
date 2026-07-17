import { TestBed } from '@angular/core/testing';
import { Type } from '@angular/core';

import { Investments } from './investments/investments';

describe('Páginas placeholder', () => {
  const cases: { component: Type<unknown>; title: string }[] = [
    { component: Investments, title: 'Investimentos' }
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
