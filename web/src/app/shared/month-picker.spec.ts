import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MonthPicker } from './month-picker';

describe('MonthPicker', () => {
  let fixture: ComponentFixture<MonthPicker>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [MonthPicker] }).compileComponents();
    fixture = TestBed.createComponent(MonthPicker);
    fixture.componentRef.setInput('month', '2026-07');
    fixture.detectChanges();
  });

  it('should render the month label in pt-BR', () => {
    expect(fixture.nativeElement.querySelector('span').textContent).toBe('Julho 2026');
  });

  it('should go to previous month when ‹ is clicked', () => {
    fixture.nativeElement.querySelectorAll('button')[0].click();
    fixture.detectChanges();

    expect(fixture.componentInstance.month()).toBe('2026-06');
  });

  it('should go to next month crossing the year when › is clicked from December', () => {
    fixture.componentRef.setInput('month', '2026-12');
    fixture.detectChanges();

    fixture.nativeElement.querySelectorAll('button')[1].click();
    fixture.detectChanges();

    expect(fixture.componentInstance.month()).toBe('2027-01');
  });
});
