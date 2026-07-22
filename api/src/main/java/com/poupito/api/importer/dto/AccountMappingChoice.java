package com.poupito.api.importer.dto;

import com.poupito.api.account.AccountType;

import java.util.UUID;

/**
 * Destino de um nome de "conta" da planilha, em ordem de precedencia:
 * existingAccountId -> conta existente; existingCardId -> cartao existente;
 * createCard != null -> cria um cartao novo (exige conta vinculada + dias);
 * senao cria uma conta nova com createType (default CHECKING).
 */
public record AccountMappingChoice(
		UUID existingAccountId,
		UUID existingCardId,
		AccountType createType,
		CreateCardChoice createCard) {

	public record CreateCardChoice(UUID accountId, Integer closingDay, Integer dueDay) {
	}

}
