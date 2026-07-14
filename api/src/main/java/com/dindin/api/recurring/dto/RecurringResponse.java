package com.dindin.api.recurring.dto;

import com.dindin.api.account.Account;
import com.dindin.api.category.Category;
import com.dindin.api.recurring.RecurringTransaction;
import com.dindin.api.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecurringResponse(
		UUID id,
		String description,
		BigDecimal amount,
		TransactionType type,
		UUID accountId,
		String accountName,
		UUID categoryId,
		String categoryName,
		String categoryIcon,
		String categoryColor,
		int dayOfMonth,
		boolean active,
		LocalDate endDate) {

	public static RecurringResponse from(RecurringTransaction recurring, Account account, Category category) {
		return new RecurringResponse(
				recurring.getId(),
				recurring.getDescription(),
				recurring.getAmount(),
				recurring.getType(),
				recurring.getAccountId(),
				account != null ? account.getName() : null,
				recurring.getCategoryId(),
				category != null ? category.getName() : null,
				category != null ? category.getIcon() : null,
				category != null ? category.getColor() : null,
				recurring.getDayOfMonth(),
				recurring.isActive(),
				recurring.getEndDate());
	}

}
