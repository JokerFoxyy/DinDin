package com.guaranin.api.importer.dto;

import com.guaranin.api.importer.ImportRow;
import com.guaranin.api.importer.ImportSection;
import com.guaranin.api.transaction.TransactionType;

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
