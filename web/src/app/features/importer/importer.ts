import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';

import { AccountService } from '../settings/account.service';
import { CardService } from '../settings/card.service';
import { CategoryService } from '../settings/category.service';
import { Account, Card, AccountType, CategoryKind } from '../settings/settings.models';
import { ImportService } from './import.service';
import { AccountMappingChoice, ImportCommitResult, ImportMapping, ImportPreview } from './import.models';

type AccountMappingMode = 'existing-account' | 'existing-card' | 'create-account' | 'create-card';

interface AccountMappingRow {
  name: string;
  mode: AccountMappingMode;
  existingAccountId: string;
  existingCardId: string;
  createType: AccountType;
  cardAccountId: string;
  cardClosingDay: number;
  cardDueDay: number;
}

interface CategoryMappingRow {
  name: string;
  mode: 'existing' | 'create';
  existingId: string;
  createKind: CategoryKind;
}

@Component({
  selector: 'app-importer',
  imports: [CurrencyPipe, DatePipe],
  templateUrl: './importer.html',
  styleUrl: './importer.css'
})
export class Importer implements OnInit {
  private readonly importService = inject(ImportService);
  private readonly accountService = inject(AccountService);
  private readonly cardService = inject(CardService);
  private readonly categoryService = inject(CategoryService);

  readonly year = signal(2026);
  readonly file = signal<File | null>(null);
  readonly preview = signal<ImportPreview | null>(null);
  readonly accounts = signal<Account[]>([]);
  readonly cards = signal<Card[]>([]);
  readonly categories = signal<{ id: string; name: string }[]>([]);
  readonly accountMappings = signal<AccountMappingRow[]>([]);
  readonly categoryMappings = signal<CategoryMappingRow[]>([]);
  readonly result = signal<ImportCommitResult | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly loading = signal(false);

  readonly accountTypes: AccountType[] = ['CHECKING', 'CASH'];
  readonly categoryKinds: CategoryKind[] = ['EXPENSE', 'INCOME'];

  ngOnInit(): void {
    this.accountService.list().subscribe((accounts) => this.accounts.set(accounts));
    this.cardService.list().subscribe((cards) => this.cards.set(cards));
    this.categoryService.list().subscribe((categories) => this.categories.set(categories));
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.file.set(input.files?.[0] ?? null);
    this.preview.set(null);
    this.result.set(null);
    this.errorMessage.set(null);
  }

  onYearChange(event: Event): void {
    const value = Number((event.target as HTMLInputElement).value);
    if (!Number.isNaN(value)) {
      this.year.set(value);
    }
  }

  analyze(): void {
    const file = this.file();
    if (!file) {
      return;
    }
    this.errorMessage.set(null);
    this.loading.set(true);
    this.importService.preview(file, this.year()).subscribe({
      next: (preview) => {
        this.preview.set(preview);
        this.accountMappings.set(preview.unmatchedAccounts.map((name) => ({
          name,
          mode: 'create-account' as AccountMappingMode,
          existingAccountId: '',
          existingCardId: '',
          createType: 'CHECKING' as AccountType,
          cardAccountId: this.accounts()[0]?.id ?? '',
          cardClosingDay: 1,
          cardDueDay: 10
        })));
        this.categoryMappings.set(preview.unmatchedCategories.map((name) => ({
          name, mode: 'create' as const, existingId: '', createKind: 'EXPENSE' as CategoryKind
        })));
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(error.error?.message ?? 'Erro ao analisar a planilha');
        this.loading.set(false);
      }
    });
  }

  updateAccountMapping(index: number, patch: Partial<AccountMappingRow>): void {
    this.accountMappings.update((list) => list.map((m, i) => (i === index ? { ...m, ...patch } : m)));
  }

  updateCategoryMapping(index: number, patch: Partial<CategoryMappingRow>): void {
    this.categoryMappings.update((list) => list.map((m, i) => (i === index ? { ...m, ...patch } : m)));
  }

  confirm(): void {
    const file = this.file();
    if (!file) {
      return;
    }
    const mapping: ImportMapping = {
      accounts: Object.fromEntries(this.accountMappings().map((m) => [m.name, this.toAccountChoice(m)])),
      categories: Object.fromEntries(this.categoryMappings().map((m) => [m.name, {
        existingCategoryId: m.mode === 'existing' ? m.existingId : null,
        createKind: m.mode === 'create' ? m.createKind : null
      }]))
    };
    this.errorMessage.set(null);
    this.loading.set(true);
    this.importService.commit(file, this.year(), mapping).subscribe({
      next: (result) => {
        this.result.set(result);
        this.preview.set(null);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(error.error?.message ?? 'Erro ao importar a planilha');
        this.loading.set(false);
      }
    });
  }

  private toAccountChoice(m: AccountMappingRow): AccountMappingChoice {
    return {
      existingAccountId: m.mode === 'existing-account' ? m.existingAccountId : null,
      existingCardId: m.mode === 'existing-card' ? m.existingCardId : null,
      createType: m.mode === 'create-account' ? m.createType : null,
      createCard: m.mode === 'create-card'
        ? { accountId: m.cardAccountId, closingDay: m.cardClosingDay, dueDay: m.cardDueDay }
        : null
    };
  }
}
