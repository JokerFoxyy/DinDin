package com.dindin.api.investment.dto;

import com.dindin.api.investment.AssetClass;
import com.dindin.api.investment.Investment;

import java.util.UUID;

public record InvestmentResponse(UUID id, String name, AssetClass assetClass, String institution) {

	public static InvestmentResponse from(Investment investment) {
		return new InvestmentResponse(investment.getId(), investment.getName(), investment.getAssetClass(),
				investment.getInstitution());
	}

}
