import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';

import { Transactions } from './transactions';
import { TransactionService } from './transaction.service';
import { AccountService } from '../settings/account.service';
import { CategoryService } from '../settings/category.service';
import { PageResponse, Transaction } from './transaction.models';
import { Account, Category } from '../settings/settings.models';

describe('Transactions', () => {
  let fixture: ComponentFixture<Transactions>;
  let component: Transactions;
  let transactionService: jasmine.SpyObj<TransactionService>;

  const accounts: Account[] = [
    { id: 'a1', name: 'Uniclass', type: 'CHECKING', closingDay: null, dueDay: null },
    { id: 'a2', name: 'Nubank', type: 'CREDIT_CARD', closingDay: 28, dueDay: 7 }
  ];
  const categories: Category[] = [
    { id: 'c1', name: 'Mercado', icon: '🛒', color: '#3fb950', kind: 'EXPENSE' },
    { id: 'c2', name: 'Salário', icon: '💰', color: '#d29922', kind: 'INCOME' }
  ];
  const padaria: Transaction = {
    id: 't1', description: 'Padaria', amount: 31.73, date: '2026-07-09', type: 'EXPENSE',
    accountId: 'a1', accountName: 'Uniclass', categoryId: 'c1', categoryName: 'Mercado',
    categoryIcon: '🛒', categoryColor: '#3fb950', invoiceMonth: null
  };
  const cartao: Transaction = {
    ...padaria, id: 't2', description: 'Streaming', accountId: 'a2', accountName: 'Nubank',
    invoiceMonth: '2026-08-01'
  };

  function pageOf(items: Transaction[], totalPages = 1): PageResponse<Transaction> {
    return { content: items, page: 0, size: 50, totalElements: items.length, totalPages };
  }

  beforeEach(async () => {
    localStorage.removeItem('dindin.lastAccount');
    transactionService = jasmine.createSpyObj<TransactionService>('TransactionService',
      ['list', 'create', 'update', 'delete']);
    transactionService.list.and.returnValue(of(pageOf([padaria, cartao])));
    const accountService = jasmine.createSpyObj<AccountService>('AccountService', ['list']);
    accountService.list.and.returnValue(of(accounts));
    const categoryService = jasmine.createSpyObj<CategoryService>('CategoryService', ['list']);
    categoryService.list.and.returnValue(of(categories));

    await TestBed.configureTestingModule({
      imports: [Transactions],
      providers: [
        { provide: TransactionService, useValue: transactionService },
        { provide: AccountService, useValue: accountService },
        { provide: CategoryService, useValue: categoryService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Transactions);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => localStorage.removeItem('dindin.lastAccount'));

  it('should render transactions with category tag and invoice hint', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Padaria');
    expect(fixture.nativeElement.querySelector('.tag').textContent).toContain('Mercado');
    expect(fixture.nativeElement.querySelector('.invoice-hint').textContent).toContain('fatura');
  });

  it('should open create modal with today and first account as defaults', () => {
    component.openCreate();

    const now = new Date();
    const todayLocal = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
    expect(component.modalOpen()).toBeTrue();
    expect(component.form.controls.date.value).toBe(todayLocal);
    expect(component.form.controls.accountId.value).toBe('a1');
    expect(component.form.controls.categoryId.value).toBe('c1');
  });

  it('should prefer the last used account when opening create modal', () => {
    localStorage.setItem('dindin.lastAccount', 'a2');

    component.openCreate();

    expect(component.form.controls.accountId.value).toBe('a2');
  });

  it('should create a transaction and remember the account', () => {
    transactionService.create.and.returnValue(of(padaria));
    component.openCreate();
    component.form.patchValue({ description: 'Padaria', amount: 31.73 });

    component.submit();

    expect(transactionService.create).toHaveBeenCalledWith(
      jasmine.objectContaining({ description: 'Padaria', amount: 31.73, accountId: 'a1' }));
    expect(localStorage.getItem('dindin.lastAccount')).toBe('a1');
    expect(component.modalOpen()).toBeFalse();
    expect(transactionService.list).toHaveBeenCalledTimes(2);
  });

  it('should not call the service when form is invalid', () => {
    component.openCreate();
    component.form.patchValue({ description: '' });

    component.submit();

    expect(transactionService.create).not.toHaveBeenCalled();
  });

  it('should switch category options when type changes to income', () => {
    component.openCreate();

    component.form.controls.type.setValue('INCOME');
    component.onTypeChange();

    expect(component.categoriesForType()).toEqual([categories[1]]);
    expect(component.form.controls.categoryId.value).toBe('c2');
  });

  it('should update the transaction when editing', () => {
    transactionService.update.and.returnValue(of(padaria));

    component.openEdit(padaria);
    component.form.patchValue({ description: 'Padaria Sameiro' });
    component.submit();

    expect(transactionService.update).toHaveBeenCalledWith('t1',
      jasmine.objectContaining({ description: 'Padaria Sameiro' }));
  });

  it('should not open edit modal for invoice adjustments', () => {
    component.openEdit({ ...padaria, type: 'INVOICE_ADJUSTMENT' });

    expect(component.modalOpen()).toBeFalse();
  });

  it('should delete a transaction and reload', () => {
    transactionService.delete.and.returnValue(of(void 0));

    component.remove(padaria);

    expect(transactionService.delete).toHaveBeenCalledWith('t1');
    expect(transactionService.list).toHaveBeenCalledTimes(2);
  });

  it('should reload from page zero when month changes', () => {
    component.goToPage(2);

    component.onMonthChange('2026-06');

    expect(component.page()).toBe(0);
    expect(transactionService.list).toHaveBeenCalledWith('2026-06', jasmine.anything(), 0);
  });

  it('should pass filters to the service when filters change', () => {
    component.filterForm.patchValue({ accountId: 'a2', type: 'EXPENSE' });

    component.onFiltersChange();

    expect(transactionService.list).toHaveBeenCalledWith(jasmine.any(String),
      jasmine.objectContaining({ accountId: 'a2', type: 'EXPENSE' }), 0);
  });

  it('should show backend message when save fails', () => {
    transactionService.create.and.returnValue(throwError(() =>
      new HttpErrorResponse({ status: 400, error: { message: 'Categoria de entrada não pode ser usada' } })));
    component.openCreate();
    component.form.patchValue({ description: 'X', amount: 10 });

    component.submit();

    expect(component.errorMessage()).toContain('Categoria de entrada');
  });
});
