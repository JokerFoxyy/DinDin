package com.guaranin.api.investment.dto;

import jakarta.validation.constraints.NotBlank;

public record InvestmentUpdateRequest(@NotBlank String name, @NotBlank String institution) {
}
