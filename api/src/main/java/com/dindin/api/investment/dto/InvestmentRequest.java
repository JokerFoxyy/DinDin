package com.dindin.api.investment.dto;

import com.dindin.api.investment.AssetClass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InvestmentRequest(
		@NotBlank String name,
		@NotNull AssetClass assetClass,
		@NotBlank String institution) {
}
