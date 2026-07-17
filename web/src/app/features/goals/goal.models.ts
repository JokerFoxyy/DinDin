export interface Goal {
  id: string;
  name: string;
  targetAmount: number;
  targetDate: string;
  accumulated: number;
  progressPercentage: number;
  requiredMonthlyContribution: number;
}

export interface GoalRequest {
  name: string;
  targetAmount: number;
  targetDate: string;
}

export interface GoalContribution {
  id: string;
  month: string;
  amount: number;
}

export interface GoalContributionRequest {
  month: string;
  amount: number;
}
