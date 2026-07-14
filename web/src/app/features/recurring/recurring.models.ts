export type RecurringType = 'EXPENSE' | 'INCOME';

export interface Recurring {
  id: string;
  description: string;
  amount: number;
  type: RecurringType;
  accountId: string;
  accountName: string | null;
  categoryId: string;
  categoryName: string | null;
  categoryIcon: string | null;
  categoryColor: string | null;
  dayOfMonth: number;
  active: boolean;
  endDate: string | null;
}

export interface RecurringPayload {
  description: string;
  amount: number;
  type: RecurringType;
  accountId: string;
  categoryId: string;
  dayOfMonth: number;
  active: boolean;
  endDate: string | null;
}

export interface Occurrence {
  recurringId: string;
  description: string;
  amount: number;
  type: RecurringType;
  accountName: string | null;
  categoryName: string | null;
  categoryIcon: string | null;
  categoryColor: string | null;
  dayOfMonth: number;
  date: string;
  transactionId: string | null;
  materialized: boolean;
  paid: boolean;
}
