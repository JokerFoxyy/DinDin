import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';

import { Investments } from './investments';
import { InvestmentService } from './investment.service';
import { Investment, InvestmentEntry, InvestmentReport } from './investment.models';

describe('Investments', () => {
  let fixture: ComponentFixture<Investments>;
  let component: Investments;
  let investmentService: jasmine.SpyObj<InvestmentService>;

  const tesouro: Investment = { id: 'i1', name: 'Tesouro Selic', assetClass: 'RENDA_FIXA', institution: 'NuInvest' };
  const report: InvestmentReport = {
    investments: [{
      id: 'i1', name: 'Tesouro Selic', assetClass: 'RENDA_FIXA', institution: 'NuInvest',
      currentBalance: 1120, lastPeriodReturnPercentage: 2
    }],
    byClass: [{ assetClass: 'RENDA_FIXA', totalBalance: 1120, lastPeriodReturnPercentage: 2 }]
  };
  const entries: InvestmentEntry[] = [
    { id: 'e1', date: '2026-01-01', type: 'ATUALIZACAO_SALDO', amount: 0, balanceAfter: 1000 },
    { id: 'e2', date: '2026-01-15', type: 'APORTE', amount: 100, balanceAfter: null },
    { id: 'e3', date: '2026-01-31', type: 'ATUALIZACAO_SALDO', amount: 0, balanceAfter: 1120 }
  ];

  function setUp(investments: Investment[], reportValue: InvestmentReport): void {
    investmentService = jasmine.createSpyObj<InvestmentService>('InvestmentService',
      ['list', 'create', 'update', 'delete', 'listEntries', 'createEntry', 'deleteEntry', 'report', 'cdi']);
    investmentService.list.and.returnValue(of(investments));
    investmentService.report.and.returnValue(of(reportValue));
    investmentService.listEntries.and.returnValue(of(entries));
    investmentService.cdi.and.returnValue(of([{ date: '2026-01-31', accumulatedPercentage: 1.2 }]));

    TestBed.resetTestingModule().configureTestingModule({
      imports: [Investments],
      providers: [{ provide: InvestmentService, useValue: investmentService }]
    }).compileComponents();

    fixture = TestBed.createComponent(Investments);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(() => setUp([tesouro], report));

  it('should render investment cards and the investment list', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Tesouro Selic');
    expect(text).toContain('Renda fixa');
  });

  it('should return the requested class performance or a zeroed default', () => {
    expect(component.classPerformance('RENDA_FIXA').totalBalance).toBe(1120);
    expect(component.classPerformance('RESERVA')).toEqual(
      { assetClass: 'RESERVA', totalBalance: 0, lastPeriodReturnPercentage: null });
  });

  it('should sum the total balance across classes', () => {
    expect(component.totalBalance()).toBe(1120);
  });

  it('should load entries and expose the most recent ones, newest first', () => {
    expect(component.recentEntries().length).toBe(3);
    expect(component.recentEntries()[0].entry.id).toBe('e3');
  });

  it('should compute chart data from the balance timeline aligned with cdi', () => {
    expect(component.hasChartData()).toBeTrue();
  });

  it('should open the create investment modal with a clean form', () => {
    component.openCreateInvestment();

    expect(component.investmentModalOpen()).toBeTrue();
    expect(component.editingInvestment()).toBeNull();
    expect(component.investmentForm.controls.name.value).toBe('');
  });

  it('should open the edit investment modal prefilled', () => {
    component.openEditInvestment(tesouro);

    expect(component.editingInvestment()).toEqual(tesouro);
    expect(component.investmentForm.controls.name.value).toBe('Tesouro Selic');
  });

  it('should create an investment and reload', () => {
    investmentService.create.and.returnValue(of(tesouro));
    component.openCreateInvestment();
    component.investmentForm.patchValue({ name: 'CDB Banco X', assetClass: 'RENDA_FIXA', institution: 'Banco X' });

    component.submitInvestment();

    expect(investmentService.create).toHaveBeenCalledWith(
      jasmine.objectContaining({ name: 'CDB Banco X', institution: 'Banco X' }));
    expect(component.investmentModalOpen()).toBeFalse();
  });

  it('should update an investment when editing', () => {
    investmentService.update.and.returnValue(of(tesouro));
    component.openEditInvestment(tesouro);
    component.investmentForm.patchValue({ name: 'Tesouro IPCA', institution: 'Rico' });

    component.submitInvestment();

    expect(investmentService.update).toHaveBeenCalledWith('i1', { name: 'Tesouro IPCA', institution: 'Rico' });
  });

  it('should not call the service when the investment form is invalid', () => {
    component.openCreateInvestment();
    component.investmentForm.patchValue({ name: '', institution: '' });

    component.submitInvestment();

    expect(investmentService.create).not.toHaveBeenCalled();
  });

  it('should show the backend error when saving an investment fails', () => {
    investmentService.create.and.returnValue(throwError(() =>
      new HttpErrorResponse({ status: 400, error: { message: 'Nome inválido' } })));
    component.openCreateInvestment();
    component.investmentForm.patchValue({ name: 'X', institution: 'Y' });

    component.submitInvestment();

    expect(component.errorMessage()).toContain('Nome inválido');
  });

  it('should fall back to a generic message when the backend error has no message', () => {
    investmentService.create.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    component.openCreateInvestment();
    component.investmentForm.patchValue({ name: 'X', institution: 'Y' });

    component.submitInvestment();

    expect(component.errorMessage()).toBe('Erro ao salvar o investimento');
  });

  it('should delete an investment and reload', () => {
    investmentService.delete.and.returnValue(of(void 0));

    component.removeInvestment(tesouro);

    expect(investmentService.delete).toHaveBeenCalledWith('i1');
  });

  it('should open the entry modal defaulting to the first investment', () => {
    component.openCreateEntry();

    expect(component.entryModalOpen()).toBeTrue();
    expect(component.entryForm.controls.investmentId.value).toBe('i1');
  });

  it('should create an entry and reload', () => {
    investmentService.createEntry.and.returnValue(of(entries[1]));
    component.openCreateEntry();
    component.entryForm.patchValue({ investmentId: 'i1', type: 'APORTE', amount: 500 });

    component.submitEntry();

    expect(investmentService.createEntry).toHaveBeenCalledWith('i1',
      jasmine.objectContaining({ type: 'APORTE', amount: 500 }));
    expect(component.entryModalOpen()).toBeFalse();
  });

  it('should require balanceAfter when registering a balance update', () => {
    component.openCreateEntry();
    component.entryForm.patchValue({ investmentId: 'i1', type: 'ATUALIZACAO_SALDO', amount: 0, balanceAfter: null });

    component.submitEntry();

    expect(investmentService.createEntry).not.toHaveBeenCalled();
    expect(component.errorMessage()).toContain('saldo');
  });

  it('should delete an entry and reload', () => {
    investmentService.deleteEntry.and.returnValue(of(void 0));

    component.removeEntry(component.recentEntries()[0]);

    expect(investmentService.deleteEntry).toHaveBeenCalledWith('i1', 'e3');
  });

  it('should not submit the entry form when it is invalid', () => {
    component.openCreateEntry();
    component.entryForm.patchValue({ investmentId: '', amount: null });

    component.submitEntry();

    expect(investmentService.createEntry).not.toHaveBeenCalled();
  });

  describe('when there are no investments', () => {
    beforeEach(() => setUp([], { investments: [], byClass: [] }));

    it('should show empty states and disable the new entry button', () => {
      expect(component.investments()).toEqual([]);
      expect(component.recentEntries()).toEqual([]);
      expect(component.hasChartData()).toBeFalse();
    });

    it('should default the total balance and weighted return to zero/null without a report', () => {
      expect(component.totalBalance()).toBe(0);
      expect(component.totalReturnPercentage()).toBeNull();
    });

    it('should default the new entry investmentId to empty string', () => {
      component.openCreateEntry();

      expect(component.entryForm.controls.investmentId.value).toBe('');
    });
  });

  it('should default totals to zero/null before the report has ever loaded', () => {
    TestBed.resetTestingModule().configureTestingModule({
      imports: [Investments],
      providers: [{ provide: InvestmentService, useValue: investmentService }]
    }).compileComponents();
    const freshFixture = TestBed.createComponent(Investments);
    const freshComponent = freshFixture.componentInstance;

    expect(freshComponent.totalBalance()).toBe(0);
    expect(freshComponent.totalReturnPercentage()).toBeNull();
  });

  describe('when an investment has no entries yet', () => {
    beforeEach(() => {
      investmentService = jasmine.createSpyObj<InvestmentService>('InvestmentService',
        ['list', 'create', 'update', 'delete', 'listEntries', 'createEntry', 'deleteEntry', 'report', 'cdi']);
      investmentService.list.and.returnValue(of([tesouro]));
      investmentService.report.and.returnValue(of(report));
      investmentService.listEntries.and.returnValue(of([]));

      TestBed.resetTestingModule().configureTestingModule({
        imports: [Investments],
        providers: [{ provide: InvestmentService, useValue: investmentService }]
      }).compileComponents();

      fixture = TestBed.createComponent(Investments);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    it('should skip the cdi request and show no chart data', () => {
      expect(investmentService.cdi).not.toHaveBeenCalled();
      expect(component.hasChartData()).toBeFalse();
    });
  });
});
