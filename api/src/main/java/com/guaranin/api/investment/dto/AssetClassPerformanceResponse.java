package com.guaranin.api.investment.dto;

import com.guaranin.api.investment.AssetClass;
import com.guaranin.api.investment.InvestmentReturnCalculator.Performance;

import java.math.BigDecimal;

public record AssetClassPerformanceResponse(
		AssetClass assetClass, BigDecimal totalBalance, BigDecimal lastPeriodReturnPercentage) {

	public static AssetClassPerformanceResponse from(AssetClass assetClass, Performance performance) {
		return new AssetClassPerformanceResponse(assetClass, performance.currentBalance(),
				performance.lastPeriodReturnPercentage());
	}

}
