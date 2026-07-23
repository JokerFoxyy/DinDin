package com.poupito.api.invoice.dto;

import com.poupito.api.invoice.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceSummaryResponse(
		UUID id,
		UUID cardId,
		String cardName,
		LocalDate month,
		LocalDate closingDate,
		LocalDate dueDate,
		BigDecimal launchedTotal,
		BigDecimal declaredTotal,
		BigDecimal adjustment,
		InvoiceStatus status) {
}
