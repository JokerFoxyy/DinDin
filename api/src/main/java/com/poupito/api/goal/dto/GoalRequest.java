package com.poupito.api.goal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalRequest(
		@NotBlank String name,
		@NotNull @DecimalMin(value = "0.01") BigDecimal targetAmount,
		@NotNull @FutureOrPresent LocalDate targetDate) {
}
