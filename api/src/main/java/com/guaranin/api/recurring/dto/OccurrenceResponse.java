package com.guaranin.api.recurring.dto;

import com.guaranin.api.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Estado de um fixo num mês específico: se já foi materializado e se está pago. */
public record OccurrenceResponse(
		UUID recurringId,
		String description,
		BigDecimal amount,
		TransactionType type,
		String accountName,
		String categoryName,
		String categoryIcon,
		String categoryColor,
		int dayOfMonth,
		LocalDate date,
		UUID transactionId,
		boolean materialized,
		boolean paid) {
}
