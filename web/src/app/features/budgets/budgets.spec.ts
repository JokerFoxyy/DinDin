import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';

import { Budgets } from './budgets';
import { BudgetService } from './budget.service';
import { CategoryService } from '../settings/category.service';
import { BudgetReport } from './budget.models';
import { Category } from '../settings/settings.models';

describe('Budgets', () => {
  let fixture: ComponentFixture<Budgets>;
  let component: Budgets;
  let budgetService: jasmine.SpyObj<BudgetService>;

  const categories: Category[] = [
    { id: 'c1', name: 'Mercado', icon: '🛒', color: '#3fb950', kind: 'EXPENSE' },
    { id: 'c2', name: 'Lazer', icon: '🎮', color: '#a371f7', kind: 'EXPENSE' },
    { id: 'c3', name: 'Salário', icon: '💰', color: '#d29922', kind: 'INCOME' }
  ];
  const mercadoBudget: BudgetReport = {
    id: 'b1', categoryId: 'c1', categoryName: 'Mercado', categoryIcon: '🛒', categoryColor: '#3fb950',
    budgeted: 500, spent: 300, percentage: 60, over: false
  };
  const lazerOver: BudgetReport = {
    id: 'b2', categoryId: 'c2', categoryName: 'Lazer', categoryIcon: '🎮', categoryColor: '#a371f7',
    budgeted: 100, spent: 150, percentage: 150, over: true
  };

  beforeEach(async () => {
    budgetService = jasmine.createSpyObj<BudgetService>('BudgetService',
      ['report', 'create', 'updateAmount', 'delete']);
    budgetService.report.and.returnValue(of([mercadoBudget, lazerOver]));
    const categoryService = jasmine.createSpyObj<CategoryService>('CategoryService', ['list']);
    categoryService.list.and.returnValue(of(categories));

    await TestBed.configureTestingModule({
      imports: [Budgets],
      providers: [
        { provide: BudgetService, useValue: budgetService },
        { provide: CategoryService, useValue: categoryService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Budgets);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should render budgets with category tag', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Mercado');
    expect(text).toContain('Lazer');
    expect(fixture.nativeElement.querySelectorAll('.tag').length).toBe(2);
  });

  it('should cap bar width at 100 when over budget', () => {
    expect(component.barWidth(lazerOver)).toBe(100);
    expect(component.barWidth(mercadoBudget)).toBe(60);
  });

  it('should list only expense categories without a budget yet as available', () => {
    expect(component.availableCategories()).toEqual([]);
  });

  it('should open create modal with first available category', async () => {
    const soloReport = jasmine.createSpyObj<BudgetService>('BudgetService',
      ['report', 'create', 'updateAmount', 'delete']);
    soloReport.report.and.returnValue(of([mercadoBudget]));
    const categoryService = jasmine.createSpyObj<CategoryService>('CategoryService', ['list']);
    categoryService.list.and.returnValue(of(categories));

    await TestBed.resetTestingModule().configureTestingModule({
      imports: [Budgets],
      providers: [
        { provide: BudgetService, useValue: soloReport },
        { provide: CategoryService, useValue: categoryService }
      ]
    }).compileComponents();
    const soloFixture = TestBed.createComponent(Budgets);
    const soloComponent = soloFixture.componentInstance;
    soloFixture.detectChanges();

    soloComponent.openCreate();

    expect(soloComponent.modalOpen()).toBeTrue();
    expect(soloComponent.form.controls.categoryId.value).toBe('c2');
  });

  it('should create a budget and reload', () => {
    budgetService.create.and.returnValue(of(mercadoBudget));
    component.openCreate();
    component.form.patchValue({ categoryId: 'c1', amount: 500 });

    component.submit();

    expect(budgetService.create).toHaveBeenCalledWith(
      jasmine.objectContaining({ categoryId: 'c1', amount: 500 }));
    expect(component.modalOpen()).toBeFalse();
    expect(budgetService.report).toHaveBeenCalledTimes(2);
  });

  it('should not call the service when form is invalid', () => {
    component.openCreate();
    component.form.patchValue({ categoryId: '', amount: null });

    component.submit();

    expect(budgetService.create).not.toHaveBeenCalled();
  });

  it('should update the amount when editing', () => {
    budgetService.updateAmount.and.returnValue(of(mercadoBudget));

    component.openEdit(mercadoBudget);
    component.form.patchValue({ amount: 800 });
    component.submit();

    expect(budgetService.updateAmount).toHaveBeenCalledWith('b1', { amount: 800 });
  });

  it('should delete a budget and reload', () => {
    budgetService.delete.and.returnValue(of(void 0));

    component.remove(mercadoBudget);

    expect(budgetService.delete).toHaveBeenCalledWith('b1');
    expect(budgetService.report).toHaveBeenCalledTimes(2);
  });

  it('should reload when month changes', () => {
    component.onMonthChange('2026-06');

    expect(budgetService.report).toHaveBeenCalledWith('2026-06');
  });

  it('should show backend message when save fails', () => {
    budgetService.create.and.returnValue(throwError(() =>
      new HttpErrorResponse({ status: 409, error: { message: 'Já existe orçamento para essa categoria neste mês' } })));
    component.openCreate();
    component.form.patchValue({ categoryId: 'c1', amount: 500 });

    component.submit();

    expect(component.errorMessage()).toContain('Já existe orçamento');
  });
});
