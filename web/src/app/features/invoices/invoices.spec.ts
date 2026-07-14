import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { Invoices } from './invoices';
import { InvoiceService } from './invoice.service';
import { InvoiceDetail, InvoiceSummary } from './invoice.models';

describe('Invoices', () => {
  let fixture: ComponentFixture<Invoices>;
  let component: Invoices;
  let invoiceService: jasmine.SpyObj<InvoiceService>;

  const openInvoice: InvoiceSummary = {
    id: 'i1', accountId: 'a1', accountName: 'Nubank', month: '2026-07-01',
    closingDate: '2026-07-28', dueDate: '2026-08-07',
    launchedTotal: 100, declaredTotal: null, adjustment: 0, status: 'OPEN'
  };
  const closedInvoice: InvoiceSummary = { ...openInvoice, declaredTotal: 150, adjustment: 50, status: 'CLOSED' };
  const detail: InvoiceDetail = {
    invoice: closedInvoice,
    transactions: [
      { id: 't1', date: '2026-07-15', description: 'Compra', amount: 100, type: 'EXPENSE',
        categoryName: 'Mercado', categoryIcon: '🛒', categoryColor: '#3fb950' },
      { id: 't2', date: '2026-07-28', description: 'Ajuste de fatura', amount: 50, type: 'INVOICE_ADJUSTMENT',
        categoryName: null, categoryIcon: null, categoryColor: null }
    ]
  };

  beforeEach(async () => {
    invoiceService = jasmine.createSpyObj<InvoiceService>('InvoiceService',
      ['list', 'detail', 'close', 'pay', 'reopen']);
    invoiceService.list.and.returnValue(of([openInvoice]));

    await TestBed.configureTestingModule({
      imports: [Invoices],
      providers: [{ provide: InvoiceService, useValue: invoiceService }]
    }).compileComponents();

    fixture = TestBed.createComponent(Invoices);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should render invoices with launched total and status', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Nubank');
    expect(text).toContain('Aberta');
    expect(fixture.nativeElement.querySelector('.status-open')).not.toBeNull();
  });

  it('should default declared input to launched total when starting to close', () => {
    component.startClose(openInvoice);

    expect(component.closingId()).toBe('i1');
    expect(component.declaredInput()).toBe('100.00');
  });

  it('should close the invoice with the declared value', () => {
    invoiceService.close.and.returnValue(of(detail));
    component.startClose(openInvoice);
    component.onDeclaredInput('150');

    component.confirmClose(openInvoice);

    expect(invoiceService.close).toHaveBeenCalledWith('i1', 150);
    expect(component.closingId()).toBeNull();
    expect(invoiceService.list).toHaveBeenCalledTimes(2);
  });

  it('should not close with an invalid declared value', () => {
    component.startClose(openInvoice);
    component.onDeclaredInput('abc');

    component.confirmClose(openInvoice);

    expect(invoiceService.close).not.toHaveBeenCalled();
    expect(component.errorMessage()).toContain('valor da fatura');
  });

  it('should pay a closed invoice', () => {
    invoiceService.pay.and.returnValue(of(detail));

    component.pay(closedInvoice);

    expect(invoiceService.pay).toHaveBeenCalledWith('i1');
  });

  it('should reopen a closed invoice', () => {
    invoiceService.reopen.and.returnValue(of(detail));

    component.reopen(closedInvoice);

    expect(invoiceService.reopen).toHaveBeenCalledWith('i1');
  });

  it('should toggle detail loading transactions', () => {
    invoiceService.detail.and.returnValue(of(detail));

    component.toggleDetail(openInvoice);

    expect(invoiceService.detail).toHaveBeenCalledWith('i1');
    expect(component.isExpanded('i1')).toBeTrue();

    component.toggleDetail(openInvoice);
    expect(component.isExpanded('i1')).toBeFalse();
  });

  it('should reload from a fresh month when month changes', () => {
    invoiceService.detail.and.returnValue(of(detail));
    component.toggleDetail(openInvoice);

    component.onMonthChange('2026-06');

    expect(component.month()).toBe('2026-06');
    expect(component.isExpanded('i1')).toBeFalse();
    expect(invoiceService.list).toHaveBeenCalledWith('2026-06');
  });

  it('should cancel the close form', () => {
    component.startClose(openInvoice);
    expect(component.closingId()).toBe('i1');

    component.cancelClose();

    expect(component.closingId()).toBeNull();
  });

  it('should reload the detail after paying when it is expanded', () => {
    invoiceService.detail.and.returnValue(of(detail));
    invoiceService.pay.and.returnValue(of(detail));
    component.toggleDetail(closedInvoice);
    expect(invoiceService.detail).toHaveBeenCalledTimes(1);

    component.pay(closedInvoice);

    expect(invoiceService.detail).toHaveBeenCalledTimes(2);
  });

  it('should map status to a css class', () => {
    expect(component.statusClass('OPEN')).toBe('status-open');
    expect(component.statusClass('PAID')).toBe('status-paid');
  });

  it('should show an error when closing fails', () => {
    invoiceService.close.and.returnValue(throwError(() => new Error('boom')));
    component.startClose(openInvoice);
    component.onDeclaredInput('150');

    component.confirmClose(openInvoice);

    expect(component.errorMessage()).toContain('Erro ao fechar');
  });
});
