package com.dindin.api.dashboard.dto;

import com.dindin.api.category.Category;

import java.math.BigDecimal;
import java.util.UUID;

public record CategorySpendResponse(
		UUID categoryId,
		String categoryName,
		String categoryIcon,
		String categoryColor,
		BigDecimal total) {

	public static CategorySpendResponse from(Category category, BigDecimal total) {
		return new CategorySpendResponse(category.getId(), category.getName(), category.getIcon(),
				category.getColor(), total);
	}

}
