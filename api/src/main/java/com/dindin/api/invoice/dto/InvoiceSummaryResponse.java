package com.dindin.api.invoice.dto;

import com.dindin.api.invoice.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceSummaryResponse(
		UUID id,
		UUID accountId,
		String accountName,
		LocalDate month,
		LocalDate closingDate,
		LocalDate dueDate,
		BigDecimal launchedTotal,
		BigDecimal declaredTotal,
		BigDecimal adjustment,
		InvoiceStatus status) {
}
