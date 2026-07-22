package com.guaranin.api.goal;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class RequiredContributionCalculatorTest {

	private final RequiredContributionCalculator calculator = new RequiredContributionCalculator();

	@Test
	void shouldDivideRemainingByMonthsRemaining() {
		BigDecimal required = calculator.compute(new BigDecimal("12000.00"), new BigDecimal("7200.00"),
				YearMonth.of(2026, 7), YearMonth.of(2026, 12));

		// (12000 - 7200) / 5 = 960.00
		assertThat(required).isEqualByComparingTo("960.00");
	}

	@Test
	void shouldRoundUp_whenDivisionIsNotExact() {
		BigDecimal required = calculator.compute(new BigDecimal("1000.00"), BigDecimal.ZERO,
				YearMonth.of(2026, 1), YearMonth.of(2026, 4));

		// 1000 / 3 = 333.33(3) -> arredonda pra cima
		assertThat(required).isEqualByComparingTo("333.34");
	}

	@Test
	void shouldReturnZero_whenAlreadyAchieved() {
		BigDecimal required = calculator.compute(new BigDecimal("1000.00"), new BigDecimal("1500.00"),
				YearMonth.of(2026, 1), YearMonth.of(2026, 12));

		assertThat(required).isEqualByComparingTo("0");
	}

	@Test
	void shouldReturnFullRemaining_whenTargetMonthIsCurrentMonth() {
		BigDecimal required = calculator.compute(new BigDecimal("1000.00"), new BigDecimal("400.00"),
				YearMonth.of(2026, 7), YearMonth.of(2026, 7));

		assertThat(required).isEqualByComparingTo("600.00");
	}

	@Test
	void shouldReturnFullRemaining_whenTargetDateAlreadyPassed() {
		BigDecimal required = calculator.compute(new BigDecimal("1000.00"), new BigDecimal("400.00"),
				YearMonth.of(2026, 7), YearMonth.of(2026, 1));

		assertThat(required).isEqualByComparingTo("600.00");
	}

}
