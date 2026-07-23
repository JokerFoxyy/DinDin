package com.poupito.api.investment.dto;

import com.poupito.api.investment.AssetClass;
import com.poupito.api.investment.Investment;

import java.util.UUID;

public record InvestmentResponse(UUID id, String name, AssetClass assetClass, String institution) {

	public static InvestmentResponse from(Investment investment) {
		return new InvestmentResponse(investment.getId(), investment.getName(), investment.getAssetClass(),
				investment.getInstitution());
	}

}
