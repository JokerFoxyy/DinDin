import { PaymentMethod } from '../settings/settings.models';

export type TransactionType = 'EXPENSE' | 'INCOME' | 'INVOICE_ADJUSTMENT' | 'INVOICE_PAYMENT';

export interface Transaction {
  id: string;
  description: string;
  amount: number;
  date: string; // yyyy-MM-dd
  type: TransactionType;
  accountId: string | null;
  accountName: string | null;
  cardId: string | null;
  cardName: string | null;
  method: PaymentMethod;
  categoryId: string | null;
  categoryName: string | null;
  categoryIcon: string | null;
  categoryColor: string | null;
  invoiceMonth: string | null; // yyyy-MM-dd (primeiro dia do mês da fatura)
  tags: string[];
  installmentNumber: number | null;
  installmentCount: number | null;
}

export interface TransactionPayload {
  description: string;
  amount: number;
  date: string;
  type: TransactionType;
  accountId?: string;
  cardId?: string;
  categoryId: string;
  tags: string[];
  installments?: number;
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
  cardId?: string;
  categoryId?: string;
  type?: TransactionType;
  q?: string;
  tag?: string;
}

export const TRANSACTION_TYPE_LABELS: Record<Exclude<TransactionType, 'INVOICE_ADJUSTMENT' | 'INVOICE_PAYMENT'>, string> = {
  EXPENSE: 'Gasto',
  INCOME: 'Entrada'
};
