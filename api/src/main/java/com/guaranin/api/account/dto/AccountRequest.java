package com.guaranin.api.account.dto;

import com.guaranin.api.account.AccountType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountRequest(
		@NotBlank @Size(max = 100) String name,
		@NotNull AccountType type,
		@Min(1) @Max(31) Integer closingDay,
		@Min(1) @Max(31) Integer dueDay) {
}
