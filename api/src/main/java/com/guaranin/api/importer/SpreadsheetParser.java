package com.guaranin.api.importer;

import com.guaranin.api.transaction.TransactionType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lê as 12 abas mensais da Planilha_Gastos_2026 (mesmo template em todas: header/seções em
 * linhas fixas, confirmado analisando o arquivo real do usuário) e extrai as linhas de gasto/
 * entrada candidatas a virar {@link com.guaranin.api.transaction.Transaction}.
 */
@Component
public class SpreadsheetParser {

	private static final Map<String, Integer> MONTH_SHEETS = new LinkedHashMap<>();

	static {
		MONTH_SHEETS.put("Janeiro", 1);
		MONTH_SHEETS.put("Fevereiro", 2);
		MONTH_SHEETS.put("Março", 3);
		MONTH_SHEETS.put("Abril", 4);
		MONTH_SHEETS.put("Maio", 5);
		MONTH_SHEETS.put("Junho", 6);
		MONTH_SHEETS.put("Julho", 7);
		MONTH_SHEETS.put("Agosto", 8);
		MONTH_SHEETS.put("Setembro", 9);
		MONTH_SHEETS.put("Outubro", 10);
		MONTH_SHEETS.put("Novembro", 11);
		MONTH_SHEETS.put("Dezembro", 12);
	}

	// Linhas 1-indexadas como aparecem na planilha; POI usa índice 0, então subtraímos 1 no uso.
	private static final int FIXOS_FIRST_ROW = 10;
	private static final int FIXOS_LAST_ROW = 24; // para antes do título/header do Cartão (25/26)
	private static final int CARTAO_FIRST_ROW = 27;
	private static final int CARTAO_LAST_ROW = 100;
	private static final int GASTOS_MES_FIRST_ROW = 10;
	private static final int GASTOS_MES_LAST_ROW = 100;
	private static final int ENTRADAS_FIRST_ROW = 9;
	private static final int ENTRADAS_LAST_ROW = 30;

	/** A seção "Entradas" da planilha não tem coluna de conta — usuário mapeia esse nome como qualquer outra conta não encontrada. */
	public static final String ENTRADAS_ACCOUNT_PLACEHOLDER = "(conta não informada — Entradas)";

	private static final List<String> ENTRADAS_BOUNDARY_PREFIXES = List.of(
			"saldo", "total", "saidas", "debito", "nubank", "click", "uniclass",
			"investimentos", "reserva", "renda fixa");

	public List<ImportRow> parse(InputStream inputStream, int year) throws IOException {
		try (Workbook workbook = WorkbookFactory.create(inputStream)) {
			FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
			List<ImportRow> rows = new ArrayList<>();
			for (Map.Entry<String, Integer> entry : MONTH_SHEETS.entrySet()) {
				Sheet sheet = workbook.getSheet(entry.getKey());
				if (sheet == null) {
					continue;
				}
				YearMonth month = YearMonth.of(year, entry.getValue());
				rows.addAll(parseFixos(sheet, evaluator, month));
				rows.addAll(parseCartao(sheet, evaluator, month));
				rows.addAll(parseGastosDoMes(sheet, evaluator, month));
				rows.addAll(parseEntradas(sheet, evaluator, month));
			}
			return rows;
		}
	}

	private List<ImportRow> parseFixos(Sheet sheet, FormulaEvaluator evaluator, YearMonth month) {
		return parseExpenseBlock(sheet, evaluator, month, sheet.getSheetName(), ImportSection.FIXOS,
				FIXOS_FIRST_ROW, FIXOS_LAST_ROW, 2, 5, 6, 7, 8);
	}

	private List<ImportRow> parseCartao(Sheet sheet, FormulaEvaluator evaluator, YearMonth month) {
		return parseExpenseBlock(sheet, evaluator, month, sheet.getSheetName(), ImportSection.CARTAO,
				CARTAO_FIRST_ROW, CARTAO_LAST_ROW, 2, 5, 6, 7, 8);
	}

	private List<ImportRow> parseGastosDoMes(Sheet sheet, FormulaEvaluator evaluator, YearMonth month) {
		List<ImportRow> rows = parseExpenseBlock(sheet, evaluator, month, sheet.getSheetName(), ImportSection.GASTOS_MES,
				GASTOS_MES_FIRST_ROW, GASTOS_MES_LAST_ROW, 10, 11, 12, 13, 14);
		return rows.stream().filter(row -> !isAjusteDeFatura(row.description())).toList();
	}

	private boolean isAjusteDeFatura(String description) {
		String normalized = stripAccents(description);
		return normalized.contains("diferen") && normalized.contains("totai");
	}

	/** Bloco genérico: descrição, data, conta(bruta), categoria(bruta), valor — sempre EXPENSE. */
	private List<ImportRow> parseExpenseBlock(Sheet sheet, FormulaEvaluator evaluator, YearMonth month, String sheetName,
			ImportSection section, int firstRow, int lastRow, int nameCol, int dateCol, int accountCol,
			int categoryCol, int amountCol) {
		List<ImportRow> rows = new ArrayList<>();
		for (int r = firstRow; r <= lastRow; r++) {
			Row row = sheet.getRow(r - 1);
			if (row == null) {
				continue;
			}
			String name = stringValue(row.getCell(nameCol));
			BigDecimal amount = numericValue(row.getCell(amountCol), evaluator);
			if (name == null || amount == null || amount.signum() <= 0) {
				continue;
			}
			String account = stringValue(row.getCell(accountCol));
			String category = stringValue(row.getCell(categoryCol));
			LocalDate date = dateFor(month, row.getCell(dateCol));
			rows.add(new ImportRow(sheetName, section, name, date, account, category, amount, TransactionType.EXPENSE));
		}
		return rows;
	}

	private List<ImportRow> parseEntradas(Sheet sheet, FormulaEvaluator evaluator, YearMonth month) {
		List<ImportRow> rows = new ArrayList<>();
		for (int r = ENTRADAS_FIRST_ROW; r <= ENTRADAS_LAST_ROW; r++) {
			Row row = sheet.getRow(r - 1);
			if (row == null) {
				continue;
			}
			String name = stringValue(row.getCell(16)); // Q
			if (name == null) {
				continue;
			}
			if (isEntradasBoundary(name)) {
				break;
			}
			BigDecimal amount = numericValue(row.getCell(17), evaluator); // R
			if (amount == null || amount.signum() <= 0) {
				continue;
			}
			LocalDate date = month.atDay(1);
			rows.add(new ImportRow(sheet.getSheetName(), ImportSection.ENTRADAS, name, date,
					ENTRADAS_ACCOUNT_PLACEHOLDER, name, amount, TransactionType.INCOME));
		}
		return rows;
	}

	private boolean isEntradasBoundary(String label) {
		String normalized = stripAccents(label);
		return ENTRADAS_BOUNDARY_PREFIXES.stream().anyMatch(normalized::startsWith);
	}

	private LocalDate dateFor(YearMonth month, Cell cell) {
		int day = 1;
		if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
			day = cell.getLocalDateTimeCellValue().getDayOfMonth();
		}
		return month.atDay(Math.min(day, month.lengthOfMonth()));
	}

	private String stringValue(Cell cell) {
		if (cell == null) {
			return null;
		}
		String value = switch (cell.getCellType()) {
			case STRING -> cell.getStringCellValue();
			case NUMERIC -> String.valueOf(cell.getNumericCellValue());
			default -> null;
		};
		if (value == null) {
			return null;
		}
		value = value.trim();
		return value.isEmpty() ? null : value;
	}

	private BigDecimal numericValue(Cell cell, FormulaEvaluator evaluator) {
		if (cell == null) {
			return null;
		}
		try {
			return switch (cell.getCellType()) {
				case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue()).setScale(2, java.math.RoundingMode.HALF_UP);
				case FORMULA -> BigDecimal.valueOf(evaluator.evaluate(cell).getNumberValue())
						.setScale(2, java.math.RoundingMode.HALF_UP);
				default -> null;
			};
		} catch (Exception e) {
			return null;
		}
	}

	private String stripAccents(String value) {
		return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT)
				.trim();
	}

}
