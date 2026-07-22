package com.poupito.api.invoice.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CloseInvoiceRequest(
		@NotNull @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal declaredTotal) {
}
