import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { MonthPicker } from '../../shared/month-picker';
import { AccountService } from '../settings/account.service';
import { CategoryService } from '../settings/category.service';
import { Account, Category } from '../settings/settings.models';
import { RecurringService } from './recurring.service';
import { Occurrence, Recurring as RecurringModel, RecurringType } from './recurring.models';

@Component({
  selector: 'app-recurring',
  imports: [ReactiveFormsModule, MonthPicker, CurrencyPipe],
  templateUrl: './recurring.html',
  styleUrl: './recurring.css'
})
export class Recurring implements OnInit {
  private readonly recurringService = inject(RecurringService);
  private readonly accountService = inject(AccountService);
  private readonly categoryService = inject(CategoryService);
  private readonly formBuilder = inject(FormBuilder);

  readonly month = signal(currentMonth());
  readonly recurrings = signal<RecurringModel[]>([]);
  readonly occurrences = signal<Occurrence[]>([]);
  readonly accounts = signal<Account[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly showForm = signal(false);
  readonly editing = signal<RecurringModel | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.formBuilder.nonNullable.group({
    description: ['', [Validators.required, Validators.maxLength(200)]],
    amount: [null as number | null, [Validators.required, Validators.min(0.01)]],
    type: ['EXPENSE' as RecurringType, Validators.required],
    accountId: ['', Validators.required],
    categoryId: ['', Validators.required],
    dayOfMonth: [1, [Validators.required, Validators.min(1), Validators.max(31)]],
    active: [true],
    endDate: ['']
  });

  ngOnInit(): void {
    this.accountService.list().subscribe((accounts) => this.accounts.set(accounts));
    this.categoryService.list().subscribe((categories) => this.categories.set(categories));
    this.load();
  }

  onMonthChange(month: string): void {
    this.month.set(month);
    this.loadOccurrences();
  }

  materialize(): void {
    this.recurringService.materialize(this.month()).subscribe({
      next: (occurrences) => this.occurrences.set(occurrences),
      error: () => this.errorMessage.set('Erro ao gerar os lançamentos do mês')
    });
  }

  occurrenceFor(recurringId: string): Occurrence | undefined {
    return this.occurrences().find((occurrence) => occurrence.recurringId === recurringId);
  }

  togglePaid(occurrence: Occurrence): void {
    if (!occurrence.transactionId) {
      return;
    }
    this.recurringService.setPaid(occurrence.transactionId, !occurrence.paid).subscribe({
      next: () => this.loadOccurrences(),
      error: () => this.errorMessage.set('Erro ao atualizar o pagamento')
    });
  }

  categoriesForType(): Category[] {
    const kind = this.form.controls.type.value === 'INCOME' ? 'INCOME' : 'EXPENSE';
    return this.categories().filter((category) => category.kind === kind);
  }

  onTypeChange(): void {
    const options = this.categoriesForType();
    if (!options.some((category) => category.id === this.form.controls.categoryId.value)) {
      this.form.controls.categoryId.setValue(options[0]?.id ?? '');
    }
  }

  openCreate(): void {
    this.editing.set(null);
    this.errorMessage.set(null);
    this.form.reset({
      description: '',
      amount: null,
      type: 'EXPENSE',
      accountId: this.accounts()[0]?.id ?? '',
      categoryId: '',
      dayOfMonth: 1,
      active: true,
      endDate: ''
    });
    this.onTypeChange();
    this.showForm.set(true);
  }

  openEdit(recurring: RecurringModel): void {
    this.editing.set(recurring);
    this.errorMessage.set(null);
    this.form.reset({
      description: recurring.description,
      amount: recurring.amount,
      type: recurring.type,
      accountId: recurring.accountId,
      categoryId: recurring.categoryId,
      dayOfMonth: recurring.dayOfMonth,
      active: recurring.active,
      endDate: recurring.endDate ?? ''
    });
    this.showForm.set(true);
  }

  cancel(): void {
    this.showForm.set(false);
    this.editing.set(null);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const payload = {
      description: raw.description,
      amount: raw.amount as number,
      type: raw.type,
      accountId: raw.accountId,
      categoryId: raw.categoryId,
      dayOfMonth: raw.dayOfMonth,
      active: raw.active,
      endDate: raw.endDate || null
    };
    const editing = this.editing();
    const request$ = editing
      ? this.recurringService.update(editing.id, payload)
      : this.recurringService.create(payload);
    request$.subscribe({
      next: () => {
        this.cancel();
        this.load();
      },
      error: (error) =>
        this.errorMessage.set(error?.error?.message ?? 'Erro ao salvar o fixo')
    });
  }

  remove(recurring: RecurringModel): void {
    this.recurringService.delete(recurring.id).subscribe({
      next: () => this.load(),
      error: () => this.errorMessage.set('Erro ao excluir o fixo')
    });
  }

  private load(): void {
    this.recurringService.list().subscribe((recurrings) => this.recurrings.set(recurrings));
    this.loadOccurrences();
  }

  private loadOccurrences(): void {
    this.recurringService.occurrences(this.month()).subscribe((occurrences) => this.occurrences.set(occurrences));
  }
}

function currentMonth(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}
