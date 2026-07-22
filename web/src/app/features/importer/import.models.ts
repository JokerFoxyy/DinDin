import { AccountType, CategoryKind } from '../settings/settings.models';

export type ImportSection = 'FIXOS' | 'CARTAO' | 'GASTOS_MES' | 'ENTRADAS';
export type ImportRowType = 'EXPENSE' | 'INCOME';

export interface ImportRow {
  sheet: string;
  section: ImportSection;
  description: string;
  date: string;
  accountName: string;
  categoryName: string | null;
  amount: number;
  type: ImportRowType;
}

export interface ImportPreview {
  rows: ImportRow[];
  unmatchedAccounts: string[];
  unmatchedCategories: string[];
}

export interface CreateCardChoice {
  accountId: string;
  closingDay: number;
  dueDay: number;
}

export interface AccountMappingChoice {
  existingAccountId: string | null;
  existingCardId: string | null;
  createType: AccountType | null;
  createCard: CreateCardChoice | null;
}

export interface CategoryMappingChoice {
  existingCategoryId: string | null;
  createKind: CategoryKind | null;
}

export interface ImportMapping {
  accounts: Record<string, AccountMappingChoice>;
  categories: Record<string, CategoryMappingChoice>;
}

export interface ImportCommitResult {
  transactionsCreated: number;
  transactionsSkippedAsDuplicate: number;
  accountsCreated: number;
  categoriesCreated: number;
}
