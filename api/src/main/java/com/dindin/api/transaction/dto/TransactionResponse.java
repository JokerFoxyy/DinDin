package com.dindin.api.transaction.dto;

import com.dindin.api.account.Account;
import com.dindin.api.category.Category;
import com.dindin.api.transaction.Transaction;
import com.dindin.api.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TransactionResponse(
		UUID id,
		String description,
		BigDecimal amount,
		LocalDate date,
		TransactionType type,
		UUID accountId,
		String accountName,
		UUID categoryId,
		String categoryName,
		String categoryIcon,
		String categoryColor,
		LocalDate invoiceMonth,
		boolean paid,
		UUID recurringId,
		List<String> tags,
		Integer installmentNumber,
		Integer installmentCount) {

	public static TransactionResponse from(Transaction transaction, Account account,
			Category category, LocalDate invoiceMonth) {
		return new TransactionResponse(
				transaction.getId(),
				transaction.getDescription(),
				transaction.getAmount(),
				transaction.getDate(),
				transaction.getType(),
				transaction.getAccountId(),
				account != null ? account.getName() : null,
				transaction.getCategoryId(),
				category != null ? category.getName() : null,
				category != null ? category.getIcon() : null,
				category != null ? category.getColor() : null,
				invoiceMonth,
				transaction.isPaid(),
				transaction.getRecurringId(),
				transaction.getTags().stream().sorted().toList(),
				transaction.getInstallmentNumber(),
				transaction.getInstallmentCount());
	}

}
