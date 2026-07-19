package com.guaranin.api.importer.support;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;

/**
 * Gera uma planilha sintética (dados fictícios) com o mesmo template fixo da
 * Planilha_Gastos_2026 real (analisada na sessão #12), só para uso em testes.
 * Colunas (0-indexadas): C=2 D=3 E=4 F=5 G=6 H=7 I=8 ... K=10 L=11 M=12 N=13 O=14 ... Q=16 R=17.
 */
public final class SampleSpreadsheetFactory {

	private SampleSpreadsheetFactory() {
	}

	/** Uma aba "Julho" com uma linha em cada seção, incluindo uma "Diferença de totais" a ser pulada. */
	public static InputStream julySample() throws IOException {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Julho");

			// Fixos: header linha 9 (índice 8) C,E,F,G,H,I; dados a partir da linha 10 (índice 9)
			Row fixosHeader = row(sheet, 8);
			set(fixosHeader, 2, "Nome");
			set(fixosHeader, 4, "Pago?");
			set(fixosHeader, 5, "Data");
			set(fixosHeader, 6, "Tipo");
			set(fixosHeader, 7, "Categoria");
			set(fixosHeader, 8, "Valor");

			expenseRow(sheet, 9, 2, "Netflix", null, "Nubank", "Assinaturas", 39.90);
			expenseRow(sheet, 10, 2, "Internet", LocalDate.of(2026, 7, 5), "Uniclass", "Contas", 120.00);

			// Cartão de Crédito: header linha 26 (índice 25) C,D,F,G,H,I; dados a partir da 27 (índice 26)
			Row cartaoHeader = row(sheet, 25);
			set(cartaoHeader, 2, "Nome");
			set(cartaoHeader, 3, "Parcelas");
			set(cartaoHeader, 5, "Data");
			set(cartaoHeader, 6, "Tipo");
			set(cartaoHeader, 7, "Categoria");
			set(cartaoHeader, 8, "Valor");

			Row cartaoData = row(sheet, 26);
			set(cartaoData, 2, "Notebook");
			set(cartaoData, 3, "10/10");
			set(cartaoData, 6, "Uniclass");
			set(cartaoData, 7, "Eletronicos");
			set(cartaoData, 8, 250.00);

			// Gastos do Mês: mesma linha 9 (índice 8), colunas K,L,M,N,O
			Row gastosMesHeader = row(sheet, 8);
			set(gastosMesHeader, 10, "Nome");
			set(gastosMesHeader, 11, "Data");
			set(gastosMesHeader, 12, "Tipo");
			set(gastosMesHeader, 13, "Categoria");
			set(gastosMesHeader, 14, "Valor");

			gastosMesRow(sheet, 9, "Diferença de totais", "Uniclass", "Contas", 50.00);
			gastosMesRow(sheet, 10, "Uber", "Nubank", "Uber/transporte", 25.00);

			// Entradas: dados a partir da linha 9 (índice 8), colunas Q,R
			Row entrada1 = row(sheet, 8);
			set(entrada1, 16, "Salário");
			set(entrada1, 17, 4000.00);

			Row entrada2 = row(sheet, 9);
			set(entrada2, 16, "Saldo mes anterior");
			set(entrada2, 17, -500.00);

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			workbook.write(out);
			return new ByteArrayInputStream(out.toByteArray());
		}
	}

	private static Row row(Sheet sheet, int rowIndex) {
		Row existing = sheet.getRow(rowIndex);
		return existing != null ? existing : sheet.createRow(rowIndex);
	}

	private static void set(Row row, int col, String value) {
		row.createCell(col).setCellValue(value);
	}

	private static void set(Row row, int col, double value) {
		row.createCell(col).setCellValue(value);
	}

	/** Linha de despesa (Fixos/Cartão/Gastos do Mês): nameCol, dateCol=+3, accountCol=+4, categoryCol=+5, amountCol=+6. */
	private static void expenseRow(Sheet sheet, int rowIndex, int nameCol, String name, LocalDate date,
			String account, String category, double amount) {
		Row row = row(sheet, rowIndex);
		set(row, nameCol, name);
		if (date != null) {
			CreationHelper helper = sheet.getWorkbook().getCreationHelper();
			CellStyle dateStyle = sheet.getWorkbook().createCellStyle();
			dateStyle.setDataFormat(helper.createDataFormat().getFormat("m/d/yy"));
			var cell = row.createCell(nameCol + 3);
			cell.setCellValue(java.sql.Date.valueOf(date));
			cell.setCellStyle(dateStyle);
		}
		set(row, nameCol + 4, account);
		set(row, nameCol + 5, category);
		set(row, nameCol + 6, amount);
	}

	/** Linha de "Gastos do Mês": colunas K,L,M,N,O são sequenciais (+0,+1,+2,+3,+4). */
	private static void gastosMesRow(Sheet sheet, int rowIndex, String name, String account, String category,
			double amount) {
		Row row = row(sheet, rowIndex);
		set(row, 10, name);
		set(row, 12, account);
		set(row, 13, category);
		set(row, 14, amount);
	}

}
