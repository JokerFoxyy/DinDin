package com.guaranin.api.importer.dto;

import java.util.Map;

public record ImportMappingRequest(
		Map<String, AccountMappingChoice> accounts,
		Map<String, CategoryMappingChoice> categories) {

	public static ImportMappingRequest empty() {
		return new ImportMappingRequest(Map.of(), Map.of());
	}

}
