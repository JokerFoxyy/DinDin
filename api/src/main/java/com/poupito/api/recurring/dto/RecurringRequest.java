package com.poupito.api.recurring.dto;

import com.poupito.api.transaction.TransactionType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecurringRequest(
		@NotBlank @Size(max = 200) String description,
		@NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount,
		@NotNull TransactionType type,
		@NotNull UUID accountId,
		@NotNull UUID categoryId,
		@NotNull @Min(1) @Max(31) Integer dayOfMonth,
		Boolean active,
		LocalDate endDate) {

	public boolean activeOrDefault() {
		return active == null || active;
	}

}
