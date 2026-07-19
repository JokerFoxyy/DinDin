package com.guaranin.api.transaction;

import java.math.BigDecimal;
import java.util.UUID;

public record CategorySpent(UUID categoryId, BigDecimal total) {
}
