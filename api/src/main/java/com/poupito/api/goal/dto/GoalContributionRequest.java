package com.poupito.api.goal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.YearMonth;

public record GoalContributionRequest(
		@NotNull @JsonFormat(pattern = "yyyy-MM") YearMonth month,
		@NotNull @DecimalMin(value = "0.01") BigDecimal amount) {
}
