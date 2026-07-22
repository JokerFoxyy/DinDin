import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { MonthPicker } from '../../shared/month-picker';
import { AccountService } from '../settings/account.service';
import { CardService } from '../settings/card.service';
import { CategoryService } from '../settings/category.service';
import { Account, Card, Category, PAYMENT_METHOD_LABELS } from '../settings/settings.models';
import { TransactionService } from './transaction.service';
import {
  TRANSACTION_TYPE_LABELS,
  Transaction,
  TransactionFilters,
  TransactionPayload,
  TransactionType
} from './transaction.models';

const LAST_TARGET_KEY = 'poupito.lastPaymentTarget';

@Component({
  selector: 'app-transactions',
  imports: [ReactiveFormsModule, MonthPicker, CurrencyPipe, DatePipe],
  templateUrl: './transactions.html',
  styleUrl: './transactions.css'
})
export class Transactions implements OnInit {
  private readonly transactionService = inject(TransactionService);
  private readonly accountService = inject(AccountService);
  private readonly cardService = inject(CardService);
  private readonly categoryService = inject(CategoryService);
  private readonly formBuilder = inject(FormBuilder);

  readonly month = signal(currentMonth());
  readonly transactions = signal<Transaction[]>([]);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly page = signal(0);
  readonly accounts = signal<Account[]>([]);
  readonly cards = signal<Card[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly modalOpen = signal(false);
  readonly editing = signal<Transaction | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly typeLabels = TRANSACTION_TYPE_LABELS;
  readonly methodLabels = PAYMENT_METHOD_LABELS;

  readonly filterForm = this.formBuilder.nonNullable.group({
    target: '',
    categoryId: '',
    type: '',
    q: '',
    tag: ''
  });

  readonly form = this.formBuilder.nonNullable.group({
    description: ['', [Validators.required, Validators.maxLength(200)]],
    amount: [null as number | null, [Validators.required, Validators.min(0.01)]],
    date: ['', Validators.required],
    type: ['EXPENSE' as TransactionType, Validators.required],
    target: ['', Validators.required],
    categoryId: ['', Validators.required],
    tags: [''],
    installments: [1, [Validators.required, Validators.min(1), Validators.max(60)]]
  });

  private searchDebounce?: ReturnType<typeof setTimeout>;

  ngOnInit(): void {
    this.accountService.list().subscribe((accounts) => this.accounts.set(accounts));
    this.cardService.list().subscribe((cards) => this.cards.set(cards));
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

  /** Busca de texto livre é debounced pra não disparar uma requisição por tecla digitada. */
  onSearchInput(): void {
    clearTimeout(this.searchDebounce);
    this.searchDebounce = setTimeout(() => this.onFiltersChange(), 300);
  }

  goToPage(page: number): void {
    this.page.set(page);
    this.load();
  }

  /** Cartão só existe no crédito, que é sempre gasto: em entradas, esconde os cartões do seletor. */
  cardsForType(): Card[] {
    return this.form.controls.type.value === 'INCOME' ? [] : this.cards();
  }

  /** Cartão selecionado no seletor "Pagar com" → habilita parcelamento e some com entradas. */
  isCardSelected(): boolean {
    return this.form.controls.target.value.startsWith('card:');
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
    // entrada não pode cair em cartão: se havia um cartão escolhido, volta pra primeira conta
    if (this.form.controls.type.value === 'INCOME' && this.isCardSelected()) {
      this.form.controls.target.setValue(this.accounts()[0] ? `account:${this.accounts()[0].id}` : '');
    }
  }

  openCreate(): void {
    this.editing.set(null);
    this.errorMessage.set(null);
    const lastTarget = localStorage.getItem(LAST_TARGET_KEY);
    const target = this.targetOptions().some((option) => option.value === lastTarget)
      ? (lastTarget as string)
      : (this.targetOptions()[0]?.value ?? '');
    this.form.reset({
      description: '',
      amount: null,
      date: todayIso(),
      type: 'EXPENSE',
      target,
      categoryId: '',
      tags: '',
      installments: 1
    });
    this.onTypeChange();
    this.modalOpen.set(true);
  }

  openEdit(transaction: Transaction): void {
    if (transaction.type === 'INVOICE_ADJUSTMENT' || transaction.type === 'INVOICE_PAYMENT') {
      return;
    }
    this.editing.set(transaction);
    this.errorMessage.set(null);
    this.form.reset({
      description: transaction.description,
      amount: transaction.amount,
      date: transaction.date,
      type: transaction.type,
      target: transaction.cardId ? `card:${transaction.cardId}` : `account:${transaction.accountId}`,
      categoryId: transaction.categoryId ?? '',
      tags: transaction.tags.join(', '),
      installments: 1
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
    const editing = this.editing();
    const onCard = raw.target.startsWith('card:');
    const targetId = raw.target.slice(raw.target.indexOf(':') + 1);
    const payload: TransactionPayload = {
      description: raw.description,
      amount: raw.amount as number,
      date: raw.date,
      type: raw.type,
      categoryId: raw.categoryId,
      tags: raw.tags.split(',').map((tag) => tag.trim()).filter((tag) => tag.length > 0),
      ...(onCard ? { cardId: targetId } : { accountId: targetId }),
      ...(!editing && onCard && raw.installments > 1 ? { installments: raw.installments } : {})
    };
    const request$ = editing
      ? this.transactionService.update(editing.id, payload)
      : this.transactionService.create(payload);
    request$.subscribe({
      next: () => {
        localStorage.setItem(LAST_TARGET_KEY, raw.target);
        this.closeModal();
        this.load();
      },
      error: (error: HttpErrorResponse) =>
        this.errorMessage.set(error.error?.message ?? 'Erro ao salvar o lançamento')
    });
  }

  remove(transaction: Transaction, scope?: 'group'): void {
    this.transactionService.delete(transaction.id, scope).subscribe({
      next: () => this.load(),
      error: () => this.errorMessage.set('Erro ao excluir o lançamento')
    });
  }

  /** Preview mostrado abaixo do campo de parcelas no formulário de criação. */
  installmentsPreview(): string | null {
    const raw = this.form.getRawValue();
    if (this.editing() || raw.installments <= 1 || !raw.amount) {
      return null;
    }
    return `${raw.installments}x de ${formatCurrency(raw.amount)}`;
  }

  /** Exporta as transações do mês com os filtros atualmente aplicados na tela. */
  export(format: 'csv' | 'xlsx'): void {
    this.transactionService.export(this.month(), this.currentFilters(), format).subscribe({
      next: (blob) => downloadBlob(blob, `transacoes-${this.month()}.${format}`),
      error: () => this.errorMessage.set('Erro ao exportar as transações')
    });
  }

  /** Opções do seletor "Pagar com": contas (débito/dinheiro) + cartões (crédito). */
  private targetOptions(): { value: string }[] {
    return [
      ...this.accounts().map((account) => ({ value: `account:${account.id}` })),
      ...this.cards().map((card) => ({ value: `card:${card.id}` }))
    ];
  }

  private currentFilters(): TransactionFilters {
    const raw = this.filterForm.getRawValue();
    const onCard = raw.target.startsWith('card:');
    const targetId = raw.target ? raw.target.slice(raw.target.indexOf(':') + 1) : '';
    return {
      accountId: raw.target && !onCard ? targetId : undefined,
      cardId: raw.target && onCard ? targetId : undefined,
      categoryId: raw.categoryId || undefined,
      type: (raw.type || undefined) as TransactionFilters['type'],
      q: raw.q || undefined,
      tag: raw.tag || undefined
    };
  }

  private load(): void {
    this.transactionService.list(this.month(), this.currentFilters(), this.page()).subscribe((result) => {
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

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
}

function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}
