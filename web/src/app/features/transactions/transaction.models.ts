export type TransactionType = 'EXPENSE' | 'INCOME' | 'INVOICE_ADJUSTMENT';

export interface Transaction {
  id: string;
  description: string;
  amount: number;
  date: string; // yyyy-MM-dd
  type: TransactionType;
  accountId: string;
  accountName: string | null;
  categoryId: string | null;
  categoryName: string | null;
  categoryIcon: string | null;
  categoryColor: string | null;
  invoiceMonth: string | null; // yyyy-MM-dd (primeiro dia do mês da fatura)
}

export interface TransactionPayload {
  description: string;
  amount: number;
  date: string;
  type: TransactionType;
  accountId: string;
  categoryId: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface TransactionFilters {
  accountId?: string;
  categoryId?: string;
  type?: TransactionType;
}

export const TRANSACTION_TYPE_LABELS: Record<Exclude<TransactionType, 'INVOICE_ADJUSTMENT'>, string> = {
  EXPENSE: 'Gasto',
  INCOME: 'Entrada'
};
