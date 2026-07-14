export interface BudgetReport {
  id: string;
  categoryId: string;
  categoryName: string;
  categoryIcon: string | null;
  categoryColor: string | null;
  budgeted: number;
  spent: number;
  percentage: number;
  over: boolean;
}

export interface BudgetCreatePayload {
  categoryId: string;
  month: string; // yyyy-MM
  amount: number;
}

export interface BudgetAmountPayload {
  amount: number;
}
