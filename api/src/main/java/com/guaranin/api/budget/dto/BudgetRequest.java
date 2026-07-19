package com.guaranin.api.budget.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

public record BudgetRequest(
		@NotNull UUID categoryId,
		@NotNull @JsonFormat(pattern = "yyyy-MM") YearMonth month,
		@NotNull @DecimalMin(value = "0.01") BigDecimal amount) {
}
