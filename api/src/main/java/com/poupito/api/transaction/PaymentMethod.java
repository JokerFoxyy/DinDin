package com.poupito.api.transaction;

import com.poupito.api.account.AccountType;

/** Método de pagamento derivado — nunca digitado pelo usuário (sessão #25). */
public enum PaymentMethod {
	CREDITO,
	DEBITO,
	DINHEIRO;

	public static PaymentMethod of(Transaction transaction, AccountType accountType) {
		if (transaction.getCardId() != null) {
			return CREDITO;
		}
		return accountType == AccountType.CASH ? DINHEIRO : DEBITO;
	}
}
