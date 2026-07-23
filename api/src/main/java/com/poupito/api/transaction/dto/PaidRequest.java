package com.poupito.api.transaction.dto;

import jakarta.validation.constraints.NotNull;

public record PaidRequest(@NotNull Boolean paid) {
}
