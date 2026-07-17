package com.dindin.api.goal;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * Regra 5 do PLANO-SDD.md: aporte mensal necessário = (target_amount - acumulado) / meses_restantes.
 */
@Component
public class RequiredContributionCalculator {

	public BigDecimal compute(BigDecimal targetAmount, BigDecimal accumulated, YearMonth currentMonth,
			YearMonth targetMonth) {
		BigDecimal remaining = targetAmount.subtract(accumulated).max(BigDecimal.ZERO);
		if (remaining.signum() == 0) {
			return BigDecimal.ZERO;
		}
		long monthsRemaining = ChronoUnit.MONTHS.between(currentMonth, targetMonth);
		if (monthsRemaining <= 0) {
			return remaining.setScale(2, RoundingMode.HALF_UP);
		}
		return remaining.divide(BigDecimal.valueOf(monthsRemaining), 2, RoundingMode.UP);
	}

}
