package com.poupito.api.investment.dto;

import com.poupito.api.investment.AssetClass;
import com.poupito.api.investment.Investment;
import com.poupito.api.investment.InvestmentReturnCalculator.Performance;

import java.math.BigDecimal;
import java.util.UUID;

public record InvestmentPerformanceResponse(
		UUID id, String name, AssetClass assetClass, String institution,
		BigDecimal currentBalance, BigDecimal lastPeriodReturnPercentage) {

	public static InvestmentPerformanceResponse from(Investment investment, Performance performance) {
		return new InvestmentPerformanceResponse(investment.getId(), investment.getName(),
				investment.getAssetClass(), investment.getInstitution(), performance.currentBalance(),
				performance.lastPeriodReturnPercentage());
	}

}
