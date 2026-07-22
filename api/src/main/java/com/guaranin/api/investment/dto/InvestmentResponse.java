package com.guaranin.api.investment.dto;

import com.guaranin.api.investment.AssetClass;
import com.guaranin.api.investment.Investment;

import java.util.UUID;

public record InvestmentResponse(UUID id, String name, AssetClass assetClass, String institution) {

	public static InvestmentResponse from(Investment investment) {
		return new InvestmentResponse(investment.getId(), investment.getName(), investment.getAssetClass(),
				investment.getInstitution());
	}

}
