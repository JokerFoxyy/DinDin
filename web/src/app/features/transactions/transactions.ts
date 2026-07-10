import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { MonthPicker } from '../../shared/month-picker';
import { AccountService } from '../settings/account.service';
import { CategoryService } from '../settings/category.service';
import { Account, Category } from '../settings/settings.models';
import { TransactionService } from './transaction.service';
import {
  TRANSACTION_TYPE_LABELS,
  Transaction,
  TransactionFilters,
  TransactionType
} from './transaction.models';

const LAST_ACCOUNT_KEY = 'dindin.lastAccount';

@Component({
  selector: 'app-transactions',
  imports: [ReactiveFormsModule, MonthPicker, CurrencyPipe, DatePipe],
  templateUrl: './transactions.html',
  styleUrl: './transactions.css'
})
export class Transactions implements OnInit {
  private readonly transactionService = inject(TransactionService);
  private readonly accountService = inject(AccountService);
  private readonly categoryService = inject(CategoryService);
  private readonly formBuilder = inject(FormBuilder);

  readonly month = signal(currentMonth());
  readonly transactions = signal<Transaction[]>([]);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly page = signal(0);
  readonly accounts = signal<Account[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly modalOpen = signal(false);
  readonly editing = signal<Transaction | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly typeLabels = TRANSACTION_TYPE_LABELS;

  readonly filterForm = this.formBuilder.nonNullable.group({
    accountId: '',
    categoryId: '',
    type: ''
  });

  readonly form = this.formBuilder.nonNullable.group({
    description: ['', [Validators.required, Validators.maxLength(200)]],
    amount: [null as number | null, [Validators.required, Validators.min(0.01)]],
    date: ['', Validators.required],
    type: ['EXPENSE' as TransactionType, Validators.required],
    accountId: ['', Validators.required],
    categoryId: ['', Validators.required]
  });

  ngOnInit(): void {
    this.accountService.list().subscribe((accounts) => this.accounts.set(accounts));
    this.categoryService.list().subscribe((categories) => this.categories.set(categories));
    this.load();
  }

  onMonthChange(month: string): void {
    this.month.set(month);
    this.page.set(0);
    this.load();
  }

  onFiltersChange(): void {
    this.page.set(0);
    this.load();
  }

  goToPage(page: number): void {
    this.page.set(page);
    this.load();
  }

  /** Categorias compatíveis com o tipo selecionado no formulário. */
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
    const lastAccount = localStorage.getItem(LAST_ACCOUNT_KEY);
    const accountId = this.accounts().some((account) => account.id === lastAccount)
      ? (lastAccount as string)
      : (this.accounts()[0]?.id ?? '');
    this.form.reset({
      description: '',
      amount: null,
      date: todayIso(),
      type: 'EXPENSE',
      accountId,
      categoryId: ''
    });
    this.onTypeChange();
    this.modalOpen.set(true);
  }

  openEdit(transaction: Transaction): void {
    if (transaction.type === 'INVOICE_ADJUSTMENT') {
      return;
    }
    this.editing.set(transaction);
    this.errorMessage.set(null);
    this.form.reset({
      description: transaction.description,
      amount: transaction.amount,
      date: transaction.date,
      type: transaction.type,
      accountId: transaction.accountId,
      categoryId: transaction.categoryId ?? ''
    });
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
    const payload = {
      description: raw.description,
      amount: raw.amount as number,
      date: raw.date,
      type: raw.type,
      accountId: raw.accountId,
      categoryId: raw.categoryId
    };
    const editing = this.editing();
    const request$ = editing
      ? this.transactionService.update(editing.id, payload)
      : this.transactionService.create(payload);
    request$.subscribe({
      next: () => {
        localStorage.setItem(LAST_ACCOUNT_KEY, payload.accountId);
        this.closeModal();
        this.load();
      },
      error: (error: HttpErrorResponse) =>
        this.errorMessage.set(error.error?.message ?? 'Erro ao salvar o lançamento')
    });
  }

  remove(transaction: Transaction): void {
    this.transactionService.delete(transaction.id).subscribe({
      next: () => this.load(),
      error: () => this.errorMessage.set('Erro ao excluir o lançamento')
    });
  }

  private load(): void {
    const raw = this.filterForm.getRawValue();
    const filters: TransactionFilters = {
      accountId: raw.accountId || undefined,
      categoryId: raw.categoryId || undefined,
      type: (raw.type || undefined) as TransactionFilters['type']
    };
    this.transactionService.list(this.month(), filters, this.page()).subscribe((result) => {
      this.transactions.set(result.content);
      this.totalElements.set(result.totalElements);
      this.totalPages.set(result.totalPages);
    });
  }
}

function currentMonth(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}

function todayIso(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
}
