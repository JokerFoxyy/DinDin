package com.dindin.api.importer.dto;

import java.util.List;

public record ImportPreviewResponse(
		List<ImportRowResponse> rows,
		List<String> unmatchedAccounts,
		List<String> unmatchedCategories) {
}
