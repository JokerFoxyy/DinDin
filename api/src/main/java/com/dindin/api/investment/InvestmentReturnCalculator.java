package com.dindin.api.investment;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * TWR simplificado (spec-app-financeiro.md): entre duas ATUALIZACAO_SALDO consecutivas,
 * rendimento = saldoAtual - saldoAnterior - fluxoLiquido(aportes - resgates no período).
 */
@Component
public class InvestmentReturnCalculator {

	public record Performance(BigDecimal currentBalance, BigDecimal lastPeriodReturnPercentage) {
	}

	public Performance compute(List<InvestmentEntry> entriesSortedByDate) {
		BigDecimal balance = BigDecimal.ZERO;
		BigDecimal previousUpdateBalance = null;
		BigDecimal netFlowSinceLastUpdate = BigDecimal.ZERO;
		BigDecimal lastReturnPercentage = null;

		for (InvestmentEntry entry : entriesSortedByDate) {
			switch (entry.getType()) {
				case APORTE -> {
					balance = balance.add(entry.getAmount());
					netFlowSinceLastUpdate = netFlowSinceLastUpdate.add(entry.getAmount());
				}
				case RESGATE -> {
					balance = balance.subtract(entry.getAmount());
					netFlowSinceLastUpdate = netFlowSinceLastUpdate.subtract(entry.getAmount());
				}
				case ATUALIZACAO_SALDO -> {
					BigDecimal newBalance = entry.getBalanceAfter();
					if (previousUpdateBalance != null && previousUpdateBalance.compareTo(BigDecimal.ZERO) > 0) {
						BigDecimal rendimento = newBalance.subtract(previousUpdateBalance)
								.subtract(netFlowSinceLastUpdate);
						lastReturnPercentage = rendimento.multiply(BigDecimal.valueOf(100))
								.divide(previousUpdateBalance, 2, RoundingMode.HALF_UP);
					}
					previousUpdateBalance = newBalance;
					balance = newBalance;
					netFlowSinceLastUpdate = BigDecimal.ZERO;
				}
			}
		}

		return new Performance(balance, lastReturnPercentage);
	}

}
