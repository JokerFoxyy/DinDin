package com.dindin.api.importer.dto;

import com.dindin.api.importer.ImportRow;
import com.dindin.api.importer.ImportSection;
import com.dindin.api.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ImportRowResponse(
		String sheet,
		ImportSection section,
		String description,
		LocalDate date,
		String accountName,
		String categoryName,
		BigDecimal amount,
		TransactionType type) {

	public static ImportRowResponse from(ImportRow row) {
		return new ImportRowResponse(row.sheet(), row.section(), row.description(), row.date(),
				row.accountNameRaw(), row.categoryNameRaw(), row.amount(), row.type());
	}

}
