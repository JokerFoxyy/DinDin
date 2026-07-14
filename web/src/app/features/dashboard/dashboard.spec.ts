import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { Dashboard } from './dashboard';
import { DashboardService } from './dashboard.service';
import { AnnualPoint, DashboardSummary } from './dashboard.models';

describe('Dashboard', () => {
  let fixture: ComponentFixture<Dashboard>;
  let component: Dashboard;
  let dashboardService: jasmine.SpyObj<DashboardService>;

  const summary: DashboardSummary = {
    income: 5000,
    expense: 3000,
    monthBalance: 2000,
    cumulativeBalance: 8000,
    categorySpend: [
      { categoryId: 'c1', categoryName: 'Mercado', categoryIcon: '🛒', categoryColor: '#3fb950', total: 300 }
    ],
    budgetReport: [
      {
        id: 'b1', categoryId: 'c1', categoryName: 'Mercado', categoryIcon: '🛒', categoryColor: '#3fb950',
        budgeted: 500, spent: 300, percentage: 60, over: false
      }
    ]
  };
  const annual: AnnualPoint[] = [
    { month: '2026-06', income: 4000, expense: 2500 },
    { month: '2026-07', income: 5000, expense: 3000 }
  ];

  beforeEach(async () => {
    dashboardService = jasmine.createSpyObj<DashboardService>('DashboardService', ['summary', 'annual']);
    dashboardService.summary.and.returnValue(of(summary));
    dashboardService.annual.and.returnValue(of(annual));

    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [{ provide: DashboardService, useValue: dashboardService }]
    }).compileComponents();

    fixture = TestBed.createComponent(Dashboard);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load summary and annual series on init', () => {
    expect(dashboardService.summary).toHaveBeenCalledWith(jasmine.any(String));
    expect(dashboardService.annual).toHaveBeenCalledWith(jasmine.any(String));
    expect(component.summary()).toEqual(summary);
    expect(component.annual()).toEqual(annual);
  });

  it('should render the four summary cards', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Entradas');
    expect(text).toContain('Gastos');
    expect(text).toContain('Saldo do mês');
    expect(text).toContain('Saldo acumulado');
  });

  it('should render the budget report table with category tag', () => {
    expect(fixture.nativeElement.querySelector('.tag').textContent).toContain('Mercado');
  });

  it('should cap bar width at 100', () => {
    expect(component.barWidth(150)).toBe(100);
    expect(component.barWidth(60)).toBe(60);
  });

  it('should reload when month changes', () => {
    component.onMonthChange('2026-06');

    expect(dashboardService.summary).toHaveBeenCalledWith('2026-06');
    expect(dashboardService.annual).toHaveBeenCalledWith('2026-06');
  });

  it('should show empty state when there is no category spend or budget', () => {
    dashboardService.summary.and.returnValue(of({
      income: 0, expense: 0, monthBalance: 0, cumulativeBalance: 0, categorySpend: [], budgetReport: []
    }));

    component.onMonthChange('2026-01');
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Nenhum gasto neste mês.');
    expect(text).toContain('Nenhum orçamento definido neste mês.');
  });

  it('should destroy charts on component destroy without throwing', () => {
    expect(() => fixture.destroy()).not.toThrow();
  });
});
