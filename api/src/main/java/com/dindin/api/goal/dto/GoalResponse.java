package com.dindin.api.goal.dto;

import com.dindin.api.goal.Goal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

public record GoalResponse(
		UUID id,
		String name,
		BigDecimal targetAmount,
		LocalDate targetDate,
		BigDecimal accumulated,
		BigDecimal progressPercentage,
		BigDecimal requiredMonthlyContribution) {

	public static GoalResponse from(Goal goal, BigDecimal accumulated, BigDecimal requiredMonthlyContribution) {
		BigDecimal progress = accumulated.multiply(BigDecimal.valueOf(100))
				.divide(goal.getTargetAmount(), 0, RoundingMode.HALF_UP)
				.min(BigDecimal.valueOf(100));
		return new GoalResponse(goal.getId(), goal.getName(), goal.getTargetAmount(), goal.getTargetDate(),
				accumulated, progress, requiredMonthlyContribution);
	}

}
