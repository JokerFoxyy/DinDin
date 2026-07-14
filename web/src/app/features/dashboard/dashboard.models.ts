import { BudgetReport } from '../budgets/budget.models';

export interface CategorySpend {
  categoryId: string;
  categoryName: string;
  categoryIcon: string | null;
  categoryColor: string | null;
  total: number;
}

export interface DashboardSummary {
  income: number;
  expense: number;
  monthBalance: number;
  cumulativeBalance: number;
  categorySpend: CategorySpend[];
  budgetReport: BudgetReport[];
}

export interface AnnualPoint {
  month: string; // yyyy-MM
  income: number;
  expense: number;
}
