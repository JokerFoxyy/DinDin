package com.dindin.api.importer.dto;

import com.dindin.api.account.AccountType;

import java.util.UUID;

/** Se {@code existingAccountId} for null, cria uma conta nova com o nome bruto e {@code createType}. */
public record AccountMappingChoice(UUID existingAccountId, AccountType createType) {
}
