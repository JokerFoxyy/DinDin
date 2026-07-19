package com.guaranin.api.goal;

import java.math.BigDecimal;
import java.util.UUID;

public record GoalContributionSum(UUID goalId, BigDecimal total) {
}
