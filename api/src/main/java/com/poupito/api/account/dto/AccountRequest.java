package com.poupito.api.account.dto;

import com.poupito.api.account.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountRequest(
		@NotBlank @Size(max = 100) String name,
		@NotNull AccountType type) {
}
