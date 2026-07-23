package com.poupito.api.importer;

import com.poupito.api.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Uma linha extraída de uma aba mensal da planilha, antes de virar transação. */
public record ImportRow(
		String sheet,
		ImportSection section,
		String description,
		LocalDate date,
		String accountNameRaw,
		String categoryNameRaw,
		BigDecimal amount,
		TransactionType type) {
}
