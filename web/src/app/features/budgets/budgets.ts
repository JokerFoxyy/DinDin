import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { MonthPicker } from '../../shared/month-picker';
import { CategoryStore } from '../../core/state/category.store';
import { Category } from '../settings/settings.models';
import { BudgetService } from './budget.service';
import { BudgetReport } from './budget.models';

@Component({
  selector: 'app-budgets',
  imports: [ReactiveFormsModule, MonthPicker, CurrencyPipe],
  templateUrl: './budgets.html',
  styleUrl: './budgets.css'
})
export class Budgets implements OnInit {
  private readonly budgetService = inject(BudgetService);
  private readonly categoryStore = inject(CategoryStore);
  private readonly formBuilder = inject(FormBuilder);

  readonly month = signal(currentMonth());
  readonly budgets = signal<BudgetReport[]>([]);
  readonly categories = this.categoryStore.categories;
  readonly modalOpen = signal(false);
  readonly editing = signal<BudgetReport | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.formBuilder.nonNullable.group({
    categoryId: ['', Validators.required],
    amount: [null as number | null, [Validators.required, Validators.min(0.01)]]
  });

  ngOnInit(): void {
    this.categoryStore.ensureLoaded();
    this.load();
  }

  onMonthChange(month: string): void {
    this.month.set(month);
    this.load();
  }

  /** Categorias de gasto que ainda não têm orçamento definido neste mês. */
  availableCategories(): Category[] {
    const budgetedIds = new Set(this.budgets().map((budget) => budget.categoryId));
    return this.categories().filter((category) => category.kind === 'EXPENSE' && !budgetedIds.has(category.id));
  }

  openCreate(): void {
    this.editing.set(null);
    this.errorMessage.set(null);
    const first = this.availableCategories()[0];
    this.form.reset({ categoryId: first?.id ?? '', amount: null });
    this.modalOpen.set(true);
  }

  openEdit(budget: BudgetReport): void {
    this.editing.set(budget);
    this.errorMessage.set(null);
    this.form.reset({ categoryId: budget.categoryId, amount: budget.budgeted });
    this.modalOpen.set(true);
  }

  closeModal(): void {
    this.modalOpen.set(false);
    this.editing.set(null);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const amount = raw.amount as number;
    const editing = this.editing();
    const request$ = editing
      ? this.budgetService.updateAmount(editing.id, { amount })
      : this.budgetService.create({ categoryId: raw.categoryId, month: this.month(), amount });
    request$.subscribe({
      next: () => {
        this.closeModal();
        this.load();
      },
      error: (error: HttpErrorResponse) =>
        this.errorMessage.set(error.error?.message ?? 'Erro ao salvar o orçamento')
    });
  }

  remove(budget: BudgetReport): void {
    this.budgetService.delete(budget.id).subscribe({
      next: () => this.load(),
      error: () => this.errorMessage.set('Erro ao excluir o orçamento')
    });
  }

  barWidth(budget: BudgetReport): number {
    return Math.min(budget.percentage, 100);
  }

  private load(): void {
    this.budgetService.report(this.month()).subscribe((budgets) => this.budgets.set(budgets));
  }
}

function currentMonth(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}
