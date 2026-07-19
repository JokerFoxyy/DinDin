package com.guaranin.api.importer;

import com.guaranin.api.importer.support.SampleSpreadsheetFactory;
import com.guaranin.api.transaction.TransactionType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpreadsheetParserTest {

	private final SpreadsheetParser parser = new SpreadsheetParser();

	@Test
	void shouldParseFixosRows() throws Exception {
		List<ImportRow> rows = parser.parse(SampleSpreadsheetFactory.julySample(), 2026);

		List<ImportRow> fixos = rows.stream().filter(r -> r.section() == ImportSection.FIXOS).toList();
		assertThat(fixos).hasSize(2);
		ImportRow netflix = fixos.stream().filter(r -> r.description().equals("Netflix")).findFirst().orElseThrow();
		assertThat(netflix.accountNameRaw()).isEqualTo("Nubank");
		assertThat(netflix.categoryNameRaw()).isEqualTo("Assinaturas");
		assertThat(netflix.amount()).isEqualByComparingTo("39.90");
		assertThat(netflix.type()).isEqualTo(TransactionType.EXPENSE);
		assertThat(netflix.date()).isEqualTo(LocalDate.of(2026, 7, 1));

		ImportRow internet = fixos.stream().filter(r -> r.description().equals("Internet")).findFirst().orElseThrow();
		assertThat(internet.date()).isEqualTo(LocalDate.of(2026, 7, 5));
	}

	@Test
	void shouldParseCartaoRow() throws Exception {
		List<ImportRow> rows = parser.parse(SampleSpreadsheetFactory.julySample(), 2026);

		List<ImportRow> cartao = rows.stream().filter(r -> r.section() == ImportSection.CARTAO).toList();
		assertThat(cartao).hasSize(1);
		assertThat(cartao.getFirst().description()).isEqualTo("Notebook");
		assertThat(cartao.getFirst().accountNameRaw()).isEqualTo("Uniclass");
		assertThat(cartao.getFirst().amount()).isEqualByComparingTo("250.00");
	}

	@Test
	void shouldSkipAjusteDeFaturaRowsInGastosDoMes() throws Exception {
		List<ImportRow> rows = parser.parse(SampleSpreadsheetFactory.julySample(), 2026);

		List<ImportRow> gastosMes = rows.stream().filter(r -> r.section() == ImportSection.GASTOS_MES).toList();
		assertThat(gastosMes).hasSize(1);
		assertThat(gastosMes.getFirst().description()).isEqualTo("Uber");
	}

	@Test
	void shouldStopEntradasAtBoundaryLabel() throws Exception {
		List<ImportRow> rows = parser.parse(SampleSpreadsheetFactory.julySample(), 2026);

		List<ImportRow> entradas = rows.stream().filter(r -> r.section() == ImportSection.ENTRADAS).toList();
		assertThat(entradas).hasSize(1);
		ImportRow salario = entradas.getFirst();
		assertThat(salario.description()).isEqualTo("Salário");
		assertThat(salario.amount()).isEqualByComparingTo("4000.00");
		assertThat(salario.type()).isEqualTo(TransactionType.INCOME);
		assertThat(salario.accountNameRaw()).isEqualTo(SpreadsheetParser.ENTRADAS_ACCOUNT_PLACEHOLDER);
	}

	@Test
	void shouldReturnEmpty_whenSheetForMonthDoesNotExist() throws Exception {
		List<ImportRow> rows = parser.parse(SampleSpreadsheetFactory.julySample(), 2026);

		assertThat(rows.stream().anyMatch(r -> r.sheet().equals("Janeiro"))).isFalse();
	}

}
