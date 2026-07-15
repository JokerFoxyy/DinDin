package com.dindin.api.investment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InvestmentReturnCalculatorTest {

	private final UUID investmentId = UUID.randomUUID();
	private final InvestmentReturnCalculator calculator = new InvestmentReturnCalculator();

	private InvestmentEntry entry(LocalDate date, EntryType type, String amount, String balanceAfter) {
		return new InvestmentEntry(investmentId, date, type, new BigDecimal(amount),
				balanceAfter == null ? null : new BigDecimal(balanceAfter));
	}

	@Test
	void shouldReturnZeroBalanceAndNullReturn_whenNoEntries() {
		InvestmentReturnCalculator.Performance performance = calculator.compute(List.of());

		assertThat(performance.currentBalance()).isEqualByComparingTo("0");
		assertThat(performance.lastPeriodReturnPercentage()).isNull();
	}

	@Test
	void shouldAccumulateBalance_whenOnlyAportesAndResgates() {
		List<InvestmentEntry> entries = List.of(
				entry(LocalDate.of(2026, 1, 5), EntryType.APORTE, "1000.00", null),
				entry(LocalDate.of(2026, 2, 5), EntryType.APORTE, "500.00", null),
				entry(LocalDate.of(2026, 3, 5), EntryType.RESGATE, "200.00", null));

		InvestmentReturnCalculator.Performance performance = calculator.compute(entries);

		assertThat(performance.currentBalance()).isEqualByComparingTo("1300.00");
		assertThat(performance.lastPeriodReturnPercentage()).isNull();
	}

	@Test
	void shouldReturnNullReturn_whenOnlyOneAtualizacaoSaldo() {
		List<InvestmentEntry> entries = List.of(
				entry(LocalDate.of(2026, 1, 5), EntryType.APORTE, "1000.00", null),
				entry(LocalDate.of(2026, 1, 31), EntryType.ATUALIZACAO_SALDO, "0", "1010.00"));

		InvestmentReturnCalculator.Performance performance = calculator.compute(entries);

		assertThat(performance.currentBalance()).isEqualByComparingTo("1010.00");
		assertThat(performance.lastPeriodReturnPercentage()).isNull();
	}

	@Test
	void shouldComputeReturn_whenAporteHappensBetweenTwoUpdates() {
		List<InvestmentEntry> entries = List.of(
				entry(LocalDate.of(2026, 1, 1), EntryType.ATUALIZACAO_SALDO, "0", "1000.00"),
				entry(LocalDate.of(2026, 1, 15), EntryType.APORTE, "100.00", null),
				entry(LocalDate.of(2026, 1, 31), EntryType.ATUALIZACAO_SALDO, "0", "1120.00"));

		InvestmentReturnCalculator.Performance performance = calculator.compute(entries);

		// rendimento = 1120 - 1000 - 100 = 20; percentual = 20/1000 * 100 = 2.00
		assertThat(performance.currentBalance()).isEqualByComparingTo("1120.00");
		assertThat(performance.lastPeriodReturnPercentage()).isEqualByComparingTo("2.00");
	}

	@Test
	void shouldNetOutResgate_whenComputingReturn() {
		List<InvestmentEntry> entries = List.of(
				entry(LocalDate.of(2026, 1, 1), EntryType.ATUALIZACAO_SALDO, "0", "1000.00"),
				entry(LocalDate.of(2026, 1, 15), EntryType.RESGATE, "100.00", null),
				entry(LocalDate.of(2026, 1, 31), EntryType.ATUALIZACAO_SALDO, "0", "920.00"));

		InvestmentReturnCalculator.Performance performance = calculator.compute(entries);

		// fluxoLiquido = -100; rendimento = 920 - 1000 - (-100) = 20; percentual = 2.00
		assertThat(performance.lastPeriodReturnPercentage()).isEqualByComparingTo("2.00");
	}

	@Test
	void shouldOnlyConsiderLastPeriod_whenMultipleUpdatesExist() {
		List<InvestmentEntry> entries = List.of(
				entry(LocalDate.of(2026, 1, 1), EntryType.ATUALIZACAO_SALDO, "0", "1000.00"),
				entry(LocalDate.of(2026, 1, 31), EntryType.ATUALIZACAO_SALDO, "0", "1100.00"),
				entry(LocalDate.of(2026, 2, 28), EntryType.ATUALIZACAO_SALDO, "0", "1089.00"));

		InvestmentReturnCalculator.Performance performance = calculator.compute(entries);

		// último período: rendimento = 1089 - 1100 = -11; percentual = -1.00
		assertThat(performance.lastPeriodReturnPercentage()).isEqualByComparingTo("-1.00");
	}

}
