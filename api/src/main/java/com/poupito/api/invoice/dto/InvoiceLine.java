package com.poupito.api.invoice.dto;

import com.poupito.api.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceLine(
		UUID id,
		LocalDate date,
		String description,
		BigDecimal amount,
		TransactionType type,
		String categoryName,
		String categoryIcon,
		String categoryColor) {
}
