package com.guaranin.api.transaction.dto;

import jakarta.validation.constraints.NotNull;

public record PaidRequest(@NotNull Boolean paid) {
}
