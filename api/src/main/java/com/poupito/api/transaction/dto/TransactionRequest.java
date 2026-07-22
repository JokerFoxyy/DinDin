package com.poupito.api.transaction.dto;

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
import java.util.List;
import java.util.UUID;

public record TransactionRequest(
		@NotBlank @Size(max = 200) String description,
		@NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount,
		@NotNull LocalDate date,
		@NotNull TransactionType type,
		@NotNull UUID accountId,
		@NotNull UUID categoryId,
		List<@Size(max = 40) String> tags,
		@Min(1) @Max(60) Integer installments) {
}
