package com.poupito.api.account;

/**
 * Onde o dinheiro vive. Cartão de crédito NÃO é conta desde a sessão #25 —
 * é instrumento de pagamento (entidade {@link com.poupito.api.card.Card}).
 */
public enum AccountType {
	CHECKING,
	CASH
}
