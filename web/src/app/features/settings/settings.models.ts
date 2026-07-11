export type AccountType = 'CHECKING' | 'CREDIT_CARD' | 'CASH';
export type CategoryKind = 'EXPENSE' | 'INCOME';

export interface Account {
  id: string;
  name: string;
  type: AccountType;
  closingDay: number | null;
  dueDay: number | null;
}

export interface Category {
  id: string;
  name: string;
  icon: string | null;
  color: string | null;
  kind: CategoryKind;
}

export const ACCOUNT_TYPE_LABELS: Record<AccountType, string> = {
  CHECKING: 'Conta corrente',
  CREDIT_CARD: 'Cartão de crédito',
  CASH: 'Dinheiro'
};

export const CATEGORY_KIND_LABELS: Record<CategoryKind, string> = {
  EXPENSE: 'Gasto',
  INCOME: 'Entrada'
};
