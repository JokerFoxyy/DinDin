package com.dindin.api.investment.dto;

import com.dindin.api.investment.EntryType;
import com.dindin.api.investment.InvestmentEntry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvestmentEntryResponse(
		UUID id, LocalDate date, EntryType type, BigDecimal amount, BigDecimal balanceAfter) {

	public static InvestmentEntryResponse from(InvestmentEntry entry) {
		return new InvestmentEntryResponse(entry.getId(), entry.getDate(), entry.getType(), entry.getAmount(),
				entry.getBalanceAfter());
	}

}
