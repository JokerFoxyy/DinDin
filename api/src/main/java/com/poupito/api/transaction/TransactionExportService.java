package com.poupito.api.transaction;

import com.poupito.api.account.Account;
import com.poupito.api.account.AccountRepository;
import com.poupito.api.card.Card;
import com.poupito.api.card.CardRepository;
import com.poupito.api.category.Category;
import com.poupito.api.category.CategoryRepository;
import com.poupito.api.invoice.CardInvoice;
import com.poupito.api.invoice.CardInvoiceRepository;
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
			"Data", "Descrição", "Conta/Cartão", "Método", "Categoria", "Tipo", "Valor", "Tags", "Parcela", "Fatura"
	};
	private static final int AMOUNT_COLUMN = 6;

	private final TransactionRepository transactionRepository;
	private final AccountRepository accountRepository;
	private final CardRepository cardRepository;
	private final CategoryRepository categoryRepository;
	private final CardInvoiceRepository cardInvoiceRepository;

	public TransactionExportService(TransactionRepository transactionRepository, AccountRepository accountRepository,
			CardRepository cardRepository, CategoryRepository categoryRepository,
			CardInvoiceRepository cardInvoiceRepository) {
		this.transactionRepository = transactionRepository;
		this.accountRepository = accountRepository;
		this.cardRepository = cardRepository;
		this.categoryRepository = categoryRepository;
		this.cardInvoiceRepository = cardInvoiceRepository;
	}

	public record ExportFile(byte[] content, String filename, String contentType) {
	}

	@Transactional(readOnly = true)
	public ExportFile export(UUID userId, YearMonth month, UUID accountId, UUID cardId, UUID categoryId,
			TransactionType type, String q, String tag, String format) {
		List<Transaction> transactions = transactionRepository.findAll(
				TransactionSpecifications.search(userId, month, accountId, cardId, categoryId, type, q, tag),
				Sort.by(Sort.Order.asc("date"), Sort.Order.asc("createdAt")));

		Map<UUID, Account> accounts = byId(transactions, Transaction::getAccountId, accountRepository::findAllById,
				Account::getId);
		Map<UUID, Card> cards = byId(transactions, Transaction::getCardId, cardRepository::findAllById, Card::getId);
		Map<UUID, Category> categories = byId(transactions, Transaction::getCategoryId,
				categoryRepository::findAllById, Category::getId);
		Map<UUID, CardInvoice> invoices = byId(transactions, Transaction::getInvoiceId,
				cardInvoiceRepository::findAllById, CardInvoice::getId);

		String filename = "transacoes-" + month + (isXlsx(format) ? ".xlsx" : ".csv");
		if (isXlsx(format)) {
			return new ExportFile(toXlsx(transactions, accounts, cards, categories, invoices), filename,
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		}
		return new ExportFile(toCsv(transactions, accounts, cards, categories, invoices), filename,
				"text/csv;charset=UTF-8");
	}

	private boolean isXlsx(String format) {
		return "xlsx".equalsIgnoreCase(format);
	}

	private byte[] toCsv(List<Transaction> transactions, Map<UUID, Account> accounts, Map<UUID, Card> cards,
			Map<UUID, Category> categories, Map<UUID, CardInvoice> invoices) {
		StringBuilder csv = new StringBuilder();
		csv.append(String.join(",", HEADERS)).append("\r\n");
		for (Transaction transaction : transactions) {
			String[] row = rowOf(transaction, accounts, cards, categories, invoices);
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

	private byte[] toXlsx(List<Transaction> transactions, Map<UUID, Account> accounts, Map<UUID, Card> cards,
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
				String[] row = rowOf(transaction, accounts, cards, categories, invoices);
				Row xlsxRow = sheet.createRow(rowIndex++);
				for (int i = 0; i < row.length; i++) {
					Cell cell = xlsxRow.createCell(i);
					if (i == AMOUNT_COLUMN) {
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

	private String[] rowOf(Transaction transaction, Map<UUID, Account> accounts, Map<UUID, Card> cards,
			Map<UUID, Category> categories, Map<UUID, CardInvoice> invoices) {
		Account account = transaction.getAccountId() != null ? accounts.get(transaction.getAccountId()) : null;
		Card card = transaction.getCardId() != null ? cards.get(transaction.getCardId()) : null;
		Category category = transaction.getCategoryId() != null ? categories.get(transaction.getCategoryId()) : null;
		CardInvoice invoice = transaction.getInvoiceId() != null ? invoices.get(transaction.getInvoiceId()) : null;
		String parcela = transaction.getInstallmentCount() != null
				? transaction.getInstallmentNumber() + "/" + transaction.getInstallmentCount()
				: "";
		return new String[] {
				transaction.getDate().format(DATE_FORMAT),
				transaction.getDescription(),
				card != null ? card.getName() : (account != null ? account.getName() : ""),
				methodLabel(transaction, account),
				category != null ? category.getName() : "",
				typeLabel(transaction.getType()),
				transaction.getAmount().toPlainString(),
				String.join("; ", transaction.getTags().stream().sorted().toList()),
				parcela,
				invoice != null ? invoice.getMonth().format(DateTimeFormatter.ofPattern("MM/yyyy")) : ""
		};
	}

	private String methodLabel(Transaction transaction, Account account) {
		return switch (PaymentMethod.of(transaction, account != null ? account.getType() : null)) {
			case CREDITO -> "Crédito";
			case DEBITO -> "Débito";
			case DINHEIRO -> "Dinheiro";
		};
	}

	private String typeLabel(TransactionType type) {
		return switch (type) {
			case EXPENSE -> "Gasto";
			case INCOME -> "Entrada";
			case INVOICE_ADJUSTMENT -> "Ajuste de fatura";
			case INVOICE_PAYMENT -> "Pagamento de fatura";
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
