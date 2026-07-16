import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';

import { Goals } from './goals';
import { GoalService } from './goal.service';
import { Goal } from './goal.models';

describe('Goals', () => {
  let fixture: ComponentFixture<Goals>;
  let component: Goals;
  let goalService: jasmine.SpyObj<GoalService>;

  const reserva: Goal = {
    id: 'g1', name: 'Reserva de emergência', targetAmount: 12000, targetDate: '2026-12-01',
    accumulated: 7200, progressPercentage: 60, requiredMonthlyContribution: 800
  };
  const achieved: Goal = {
    id: 'g2', name: 'Viagem', targetAmount: 1000, targetDate: '2026-08-01',
    accumulated: 1000, progressPercentage: 100, requiredMonthlyContribution: 0
  };

  beforeEach(() => {
    goalService = jasmine.createSpyObj<GoalService>('GoalService',
      ['list', 'create', 'update', 'delete', 'listContributions', 'createContribution', 'deleteContribution']);
    goalService.list.and.returnValue(of([reserva, achieved]));

    TestBed.configureTestingModule({
      imports: [Goals],
      providers: [{ provide: GoalService, useValue: goalService }]
    }).compileComponents();

    fixture = TestBed.createComponent(Goals);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should render the goals with progress', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Reserva de emergência');
    expect(text).toContain('Viagem');
  });

  it('should cap the bar width at 100', () => {
    expect(component.barWidth(reserva)).toBe(60);
    expect(component.barWidth({ ...reserva, progressPercentage: 140 })).toBe(100);
  });

  it('should show the required contribution label with month/year', () => {
    expect(component.requiredContributionLabel(reserva)).toContain('dez/2026');
    expect(component.requiredContributionLabel(reserva)).toContain('mês');
  });

  it('should show an achieved message when the goal is complete', () => {
    expect(component.requiredContributionLabel(achieved)).toBe('Meta atingida 🎉');
  });

  it('should open the create goal modal with a clean form', () => {
    component.openCreateGoal();

    expect(component.goalModalOpen()).toBeTrue();
    expect(component.editingGoal()).toBeNull();
    expect(component.goalForm.controls.name.value).toBe('');
  });

  it('should open the edit goal modal prefilled', () => {
    component.openEditGoal(reserva);

    expect(component.editingGoal()).toEqual(reserva);
    expect(component.goalForm.controls.name.value).toBe('Reserva de emergência');
  });

  it('should create a goal and reload', () => {
    goalService.create.and.returnValue(of(reserva));
    component.openCreateGoal();
    component.goalForm.patchValue({ name: 'Carro', targetAmount: 30000, targetDate: '2028-01-01' });

    component.submitGoal();

    expect(goalService.create).toHaveBeenCalledWith(
      jasmine.objectContaining({ name: 'Carro', targetAmount: 30000, targetDate: '2028-01-01' }));
    expect(component.goalModalOpen()).toBeFalse();
  });

  it('should update a goal when editing', () => {
    goalService.update.and.returnValue(of(reserva));
    component.openEditGoal(reserva);
    component.goalForm.patchValue({ targetAmount: 15000 });

    component.submitGoal();

    expect(goalService.update).toHaveBeenCalledWith('g1', jasmine.objectContaining({ targetAmount: 15000 }));
  });

  it('should not call the service when the goal form is invalid', () => {
    component.openCreateGoal();
    component.goalForm.patchValue({ name: '', targetAmount: null });

    component.submitGoal();

    expect(goalService.create).not.toHaveBeenCalled();
  });

  it('should show the backend error when saving a goal fails', () => {
    goalService.create.and.returnValue(throwError(() =>
      new HttpErrorResponse({ status: 400, error: { message: 'Nome inválido' } })));
    component.openCreateGoal();
    component.goalForm.patchValue({ name: 'X', targetAmount: 100, targetDate: '2027-01-01' });

    component.submitGoal();

    expect(component.errorMessage()).toContain('Nome inválido');
  });

  it('should fall back to a generic message when the backend error has no message', () => {
    goalService.create.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    component.openCreateGoal();
    component.goalForm.patchValue({ name: 'X', targetAmount: 100, targetDate: '2027-01-01' });

    component.submitGoal();

    expect(component.errorMessage()).toBe('Erro ao salvar a meta');
  });

  it('should delete a goal and reload', () => {
    goalService.delete.and.returnValue(of(void 0));

    component.removeGoal(reserva);

    expect(goalService.delete).toHaveBeenCalledWith('g1');
  });

  it('should open the contribution modal for the selected goal', () => {
    component.openCreateContribution(reserva);

    expect(component.contributionModalOpen()).toBeTrue();
    expect(component.contributingGoal()).toEqual(reserva);
  });

  it('should create a contribution and reload', () => {
    goalService.createContribution.and.returnValue(of({ id: 'c1', month: '2026-07-01', amount: 800 }));
    component.openCreateContribution(reserva);
    component.contributionForm.patchValue({ month: '2026-07', amount: 800 });

    component.submitContribution();

    expect(goalService.createContribution).toHaveBeenCalledWith('g1',
      jasmine.objectContaining({ month: '2026-07', amount: 800 }));
    expect(component.contributionModalOpen()).toBeFalse();
  });

  it('should not submit the contribution form when it is invalid', () => {
    component.openCreateContribution(reserva);
    component.contributionForm.patchValue({ amount: null });

    component.submitContribution();

    expect(goalService.createContribution).not.toHaveBeenCalled();
  });

  it('should show the backend error when saving a contribution fails', () => {
    goalService.createContribution.and.returnValue(throwError(() =>
      new HttpErrorResponse({ status: 400, error: { message: 'Valor inválido' } })));
    component.openCreateContribution(reserva);
    component.contributionForm.patchValue({ amount: 800 });

    component.submitContribution();

    expect(component.errorMessage()).toContain('Valor inválido');
  });

  describe('when there are no goals', () => {
    beforeEach(() => {
      goalService.list.and.returnValue(of([]));
      fixture = TestBed.createComponent(Goals);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    it('should show the empty state', () => {
      const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(text).toContain('Nenhuma meta cadastrada ainda.');
    });
  });
});
