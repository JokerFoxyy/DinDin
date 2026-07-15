package com.dindin.api.investment.dto;

import java.util.List;

public record InvestmentReportResponse(
		List<InvestmentPerformanceResponse> investments, List<AssetClassPerformanceResponse> byClass) {
}
