package com.guaranin.api.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.YearMonth;

public record AnnualPointResponse(
		@JsonFormat(pattern = "yyyy-MM") YearMonth month,
		BigDecimal income,
		BigDecimal expense) {
}
