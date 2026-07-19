package com.guaranin.api.budget.dto;

import com.guaranin.api.budget.Budget;
import com.guaranin.api.category.Category;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public record BudgetReportResponse(
		UUID id,
		UUID categoryId,
		String categoryName,
		String categoryIcon,
		String categoryColor,
		BigDecimal budgeted,
		BigDecimal spent,
		BigDecimal percentage,
		boolean over) {

	public static BudgetReportResponse from(Budget budget, Category category, BigDecimal spent) {
		BigDecimal budgeted = budget.getAmount();
		BigDecimal percentage = spent.multiply(BigDecimal.valueOf(100))
				.divide(budgeted, 0, RoundingMode.HALF_UP);
		return new BudgetReportResponse(budget.getId(), category.getId(), category.getName(),
				category.getIcon(), category.getColor(), budgeted, spent, percentage,
				spent.compareTo(budgeted) > 0);
	}

}
