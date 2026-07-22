package com.poupito.api.investment.dto;

import com.poupito.api.investment.EntryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvestmentEntryRequest(
		@NotNull LocalDate date,
		@NotNull EntryType type,
		@NotNull @DecimalMin(value = "0", inclusive = true) BigDecimal amount,
		BigDecimal balanceAfter) {
}
