package com.poupito.api.importer.dto;

public record ImportCommitResponse(
		int transactionsCreated,
		int transactionsSkippedAsDuplicate,
		int accountsCreated,
		int categoriesCreated) {
}
