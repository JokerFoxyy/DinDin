package com.dindin.api.budget.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BudgetAmountRequest(@NotNull @DecimalMin(value = "0.01") BigDecimal amount) {
}
