package com.poupito.api.invoice.dto;

import java.util.UUID;

/** Conta que paga a fatura; se nula, usa a conta vinculada do cartão. */
public record PayInvoiceRequest(UUID accountId) {
}
