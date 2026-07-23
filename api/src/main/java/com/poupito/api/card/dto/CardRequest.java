package com.poupito.api.card.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CardRequest(
		@NotBlank @Size(max = 100) String name,
		@NotNull UUID accountId,
		@NotNull @Min(1) @Max(31) Integer closingDay,
		@NotNull @Min(1) @Max(31) Integer dueDay) {
}
