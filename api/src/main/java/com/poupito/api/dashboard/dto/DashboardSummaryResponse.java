package com.poupito.api.dashboard.dto;

import com.poupito.api.budget.dto.BudgetReportResponse;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryResponse(
		BigDecimal income,
		BigDecimal expense,
		BigDecimal monthBalance,
		BigDecimal cumulativeBalance,
		List<CategorySpendResponse> categorySpend,
		List<BudgetReportResponse> budgetReport) {
}
