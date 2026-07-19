package com.guaranin.api.cdi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CdiPointResponse(LocalDate date, BigDecimal accumulatedPercentage) {
}
