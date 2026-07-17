package com.dindin.api.transaction;

import com.dindin.api.account.Account;
import com.dindin.api.account.AccountRepository;
import com.dindin.api.category.Category;
import com.dindin.api.category.CategoryRepository;
import com.dindin.api.invoice.CardInvoice;
import com.dindin.api.invoice.CardInvoiceRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TransactionExportService {

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final String[] HEADERS = {
			"Data", "Descrição", "Conta", "Categoria", "Tipo", "Valor", "Tags", "Parcela", "Fatura"
	};

	private final TransactionRepository transactionRepository;
	private final AccountRepository accountRepository;
	private final CategoryRepository categoryRepository;
	private final CardInvoiceRepository cardInvoiceRepository;

	public TransactionExportService(TransactionRepository transactionRepository, AccountRepository accountRepository,
			CategoryRepository categoryRepository, CardInvoiceRepository cardInvoiceRepository) {
		this.transactionRepository = transactionRepository;
		this.accountRepository = accountRepository;
		this.categoryRepository = categoryRepository;
		this.cardInvoiceRepository = cardInvoiceRepository;
	}

	public record ExportFile(byte[] content, String filename, String contentType) {
	}

	@Transactional(readOnly = true)
	public ExportFile export(UUID userId, YearMonth month, UUID accountId, UUID categoryId, TransactionType type,
			String q, String tag, String format) {
		List<Transaction> transactions = transactionRepository.findAll(
				TransactionSpecifications.search(userId, month, accountId, categoryId, type, q, tag),
				Sort.by(Sort.Order.asc("date"), Sort.Order.asc("createdAt")));

		Map<UUID, Account> accounts = byId(transactions, Transaction::getAccountId, accountRepository::findAllById,
				Account::getId);
		Map<UUID, Category> categories = byId(transactions, Transaction::getCategoryId,
				categoryRepository::findAllById, Category::getId);
		Map<UUID, CardInvoice> invoices = byId(transactions, Transaction::getInvoiceId,
				cardInvoiceRepository::findAllById, CardInvoice::getId);

		String filename = "transacoes-" + month + (isXlsx(format) ? ".xlsx" : ".csv");
		if (isXlsx(format)) {
			return new ExportFile(toXlsx(transactions, accounts, categories, invoices), filename,
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		}
		return new ExportFile(toCsv(transactions, accounts, categories, invoices), filename, "text/csv;charset=UTF-8");
	}

	private boolean isXlsx(String format) {
		return "xlsx".equalsIgnoreCase(format);
	}

	private byte[] toCsv(List<Transaction> transactions, Map<UUID, Account> accounts,
			Map<UUID, Category> categories, Map<UUID, CardInvoice> invoices) {
		StringBuilder csv = new StringBuilder();
		csv.append(String.join(",", HEADERS)).append("\r\n");
		for (Transaction transaction : transactions) {
			String[] row = rowOf(transaction, accounts, categories, invoices);
			csv.append(String.join(",", java.util.Arrays.stream(row).map(this::escapeCsv).toList())).append("\r\n");
		}
		return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
	}

	private String escapeCsv(String value) {
		if (value == null) {
			return "";
		}
		if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
	}

	private byte[] toXlsx(List<Transaction> transactions, Map<UUID, Account> accounts,
			Map<UUID, Category> categories, Map<UUID, CardInvoice> invoices) {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Transações");
			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			headerFont.setFontName("Arial");
			CellStyle headerStyle = workbook.createCellStyle();
			headerStyle.setFont(headerFont);

			Font bodyFont = workbook.createFont();
			bodyFont.setFontName("Arial");
			CellStyle textStyle = workbook.createCellStyle();
			textStyle.setFont(bodyFont);
			DataFormat dataFormat = workbook.createDataFormat();
			CellStyle amountStyle = workbook.createCellStyle();
			amountStyle.setFont(bodyFont);
			amountStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));

			Row header = sheet.createRow(0);
			for (int i = 0; i < HEADERS.length; i++) {
				Cell cell = header.createCell(i);
				cell.setCellValue(HEADERS[i]);
				cell.setCellStyle(headerStyle);
			}

			int rowIndex = 1;
			for (Transaction transaction : transactions) {
				String[] row = rowOf(transaction, accounts, categories, invoices);
				Row xlsxRow = sheet.createRow(rowIndex++);
				for (int i = 0; i < row.length; i++) {
					Cell cell = xlsxRow.createCell(i);
					if (i == 5) {
						cell.setCellValue(transaction.getAmount().doubleValue());
						cell.setCellStyle(amountStyle);
					} else {
						cell.setCellValue(row[i]);
						cell.setCellStyle(textStyle);
					}
				}
			}
			for (int i = 0; i < HEADERS.length; i++) {
				sheet.autoSizeColumn(i);
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			workbook.write(out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException("Falha ao gerar planilha de export", e);
		}
	}

	private String[] rowOf(Transaction transaction, Map<UUID, Account> accounts, Map<UUID, Category> categories,
			Map<UUID, CardInvoice> invoices) {
		Account account = accounts.get(transaction.getAccountId());
		Category category = transaction.getCategoryId() != null ? categories.get(transaction.getCategoryId()) : null;
		CardInvoice invoice = transaction.getInvoiceId() != null ? invoices.get(transaction.getInvoiceId()) : null;
		String parcela = transaction.getInstallmentCount() != null
				? transaction.getInstallmentNumber() + "/" + transaction.getInstallmentCount()
				: "";
		return new String[] {
				transaction.getDate().format(DATE_FORMAT),
				transaction.getDescription(),
				account != null ? account.getName() : "",
				category != null ? category.getName() : "",
				typeLabel(transaction.getType()),
				transaction.getAmount().toPlainString(),
				String.join("; ", transaction.getTags().stream().sorted().toList()),
				parcela,
				invoice != null ? invoice.getMonth().format(DateTimeFormatter.ofPattern("MM/yyyy")) : ""
		};
	}

	private String typeLabel(TransactionType type) {
		return switch (type) {
			case EXPENSE -> "Gasto";
			case INCOME -> "Entrada";
			case INVOICE_ADJUSTMENT -> "Ajuste de fatura";
		};
	}

	private <E> Map<UUID, E> byId(List<Transaction> transactions, Function<Transaction, UUID> idExtractor,
			Function<Iterable<UUID>, Iterable<E>> loader, Function<E, UUID> entityId) {
		var ids = transactions.stream()
				.map(idExtractor)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		if (ids.isEmpty()) {
			return Map.of();
		}
		var result = new java.util.HashMap<UUID, E>();
		loader.apply(ids).forEach(entity -> result.put(entityId.apply(entity), entity));
		return result;
	}

}
