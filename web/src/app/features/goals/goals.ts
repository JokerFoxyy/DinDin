import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { GoalService } from './goal.service';
import { Goal } from './goal.models';

@Component({
  selector: 'app-goals',
  imports: [ReactiveFormsModule, CurrencyPipe],
  templateUrl: './goals.html',
  styleUrl: './goals.css'
})
export class Goals implements OnInit {
  private readonly goalService = inject(GoalService);
  private readonly formBuilder = inject(FormBuilder);

  readonly goals = signal<Goal[]>([]);
  readonly errorMessage = signal<string | null>(null);

  readonly goalModalOpen = signal(false);
  readonly editingGoal = signal<Goal | null>(null);
  readonly contributionModalOpen = signal(false);
  readonly contributingGoal = signal<Goal | null>(null);

  readonly goalForm = this.formBuilder.nonNullable.group({
    name: ['', Validators.required],
    targetAmount: [null as number | null, [Validators.required, Validators.min(0.01)]],
    targetDate: ['', Validators.required]
  });

  readonly contributionForm = this.formBuilder.nonNullable.group({
    month: [currentMonth(), Validators.required],
    amount: [null as number | null, [Validators.required, Validators.min(0.01)]]
  });

  ngOnInit(): void {
    this.load();
  }

  barWidth(goal: Goal): number {
    return Math.min(goal.progressPercentage, 100);
  }

  requiredContributionLabel(goal: Goal): string {
    if (goal.requiredMonthlyContribution <= 0) {
      return 'Meta atingida 🎉';
    }
    return `Aporte necessário: ${formatCurrency(goal.requiredMonthlyContribution)}/mês até ${monthYearLabel(goal.targetDate)}`;
  }

  openCreateGoal(): void {
    this.editingGoal.set(null);
    this.errorMessage.set(null);
    this.goalForm.reset({ name: '', targetAmount: null, targetDate: '' });
    this.goalModalOpen.set(true);
  }

  openEditGoal(goal: Goal): void {
    this.editingGoal.set(goal);
    this.errorMessage.set(null);
    this.goalForm.reset({ name: goal.name, targetAmount: goal.targetAmount, targetDate: goal.targetDate });
    this.goalModalOpen.set(true);
  }

  closeGoalModal(): void {
    this.goalModalOpen.set(false);
    this.editingGoal.set(null);
  }

  submitGoal(): void {
    if (this.goalForm.invalid) {
      this.goalForm.markAllAsTouched();
      return;
    }
    const raw = this.goalForm.getRawValue();
    const request = { name: raw.name, targetAmount: raw.targetAmount as number, targetDate: raw.targetDate };
    const editing = this.editingGoal();
    const request$ = editing ? this.goalService.update(editing.id, request) : this.goalService.create(request);
    request$.subscribe({
      next: () => {
        this.closeGoalModal();
        this.load();
      },
      error: (error: HttpErrorResponse) => this.errorMessage.set(error.error?.message ?? 'Erro ao salvar a meta')
    });
  }

  removeGoal(goal: Goal): void {
    this.goalService.delete(goal.id).subscribe({
      next: () => this.load(),
      error: () => this.errorMessage.set('Erro ao excluir a meta')
    });
  }

  openCreateContribution(goal: Goal): void {
    this.contributingGoal.set(goal);
    this.errorMessage.set(null);
    this.contributionForm.reset({ month: currentMonth(), amount: null });
    this.contributionModalOpen.set(true);
  }

  closeContributionModal(): void {
    this.contributionModalOpen.set(false);
    this.contributingGoal.set(null);
  }

  submitContribution(): void {
    if (this.contributionForm.invalid) {
      this.contributionForm.markAllAsTouched();
      return;
    }
    const goal = this.contributingGoal();
    if (!goal) {
      return;
    }
    const raw = this.contributionForm.getRawValue();
    this.goalService.createContribution(goal.id, { month: raw.month, amount: raw.amount as number }).subscribe({
      next: () => {
        this.closeContributionModal();
        this.load();
      },
      error: (error: HttpErrorResponse) => this.errorMessage.set(error.error?.message ?? 'Erro ao registrar o aporte')
    });
  }

  private load(): void {
    this.goalService.list().subscribe((goals) => this.goals.set(goals));
  }
}

function currentMonth(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}

function monthYearLabel(isoDate: string): string {
  const [year, month] = isoDate.split('-').map(Number);
  const name = new Intl.DateTimeFormat('pt-BR', { month: 'short' }).format(new Date(year, month - 1, 1));
  return `${name.replace('.', '')}/${year}`;
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
}
