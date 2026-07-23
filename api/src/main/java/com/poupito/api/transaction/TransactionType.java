package com.poupito.api.transaction;

public enum TransactionType {
	EXPENSE,
	INCOME,
	/** Reservado: criado pelo fechamento de fatura quando o declarado difere do lançado. */
	INVOICE_ADJUSTMENT,
	/** Reservado: criado ao pagar uma fatura — debita a conta vinculada (caixa) e é excluído dos gastos (competência). */
	INVOICE_PAYMENT
}
