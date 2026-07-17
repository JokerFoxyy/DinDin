package com.dindin.api.goal.dto;

import com.dindin.api.goal.GoalContribution;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GoalContributionResponse(UUID id, LocalDate month, BigDecimal amount) {

	public static GoalContributionResponse from(GoalContribution contribution) {
		return new GoalContributionResponse(contribution.getId(), contribution.getMonth(), contribution.getAmount());
	}

}
