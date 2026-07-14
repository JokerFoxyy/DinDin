package com.dindin.api.transaction.dto;

import jakarta.validation.constraints.NotNull;

public record PaidRequest(@NotNull Boolean paid) {
}
