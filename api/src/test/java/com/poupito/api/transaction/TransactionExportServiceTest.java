package com.poupito.api.transaction;

import com.poupito.api.account.Account;
import com.poupito.api.account.AccountRepository;
import com.poupito.api.account.AccountType;
import com.poupito.api.category.Category;
import com.poupito.api.category.CategoryKind;
import com.poupito.api.category.CategoryRepository;
import com.poupito.api.invoice.CardInvoiceRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionExportServiceTest {

	private final UUID userId = UUID.randomUUID();

	@Mock
	private TransactionRepository transactionRepository;
	@Mock
	private AccountRepository accountRepository;
	@Mock
	private CategoryRepository categoryRepository;
	@Mock
	private CardInvoiceRepository cardInvoiceRepository;

	private TransactionExportService service;

	private Account account;
	private Category category;
	private Transaction transaction;

	@BeforeEach
	void setUp() {
		service = new TransactionExportService(transactionRepository, accountRepository, categoryRepository,
				cardInvoiceRepository);
		account = new Account(userId, "Uniclass", AccountType.CHECKING, null, null);
		ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
		category = new Category(userId, "Mercado, Doces", "🛒", "#3fb950", CategoryKind.EXPENSE);
		ReflectionTestUtils.setField(category, "id", UUID.randomUUID());
		transaction = new Transaction(userId, account.getId(), category.getId(), null,
				"Padaria \"Sameiro\"", new BigDecimal("31.73"), LocalDate.of(2026, 7, 9), TransactionType.EXPENSE);
		transaction.updateTags(java.util.Set.of("viagem"));

		lenient().when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
				.thenReturn(List.of(transaction));
		lenient().when(accountRepository.findAllById(any())).thenReturn(List.of(account));
		lenient().when(categoryRepository.findAllById(any())).thenReturn(List.of(category));
	}

	@Test
	void shouldGenerateCsv_withHeaderAndEscapedFields() {
		TransactionExportService.ExportFile file = service.export(userId, YearMonth.of(2026, 7), null, null, null,
				null, null, "csv");

		String csv = new String(file.content(), java.nio.charset.StandardCharsets.UTF_8);
		assertThat(file.filename()).isEqualTo("transacoes-2026-07.csv");
		assertThat(file.contentType()).startsWith("text/csv");
		assertThat(csv).startsWith("Data,Descrição,Conta,Categoria,Tipo,Valor,Tags,Parcela,Fatura\r\n");
		// descrição tem aspas -> precisa escapar; categoria tem vírgula -> precisa aspas
		assertThat(csv).contains("\"Padaria \"\"Sameiro\"\"\"");
		assertThat(csv).contains("\"Mercado, Doces\"");
		assertThat(csv).contains("09/07/2026");
		assertThat(csv).contains("Uniclass");
		assertThat(csv).contains("Gasto");
		assertThat(csv).contains("31.73");
		assertThat(csv).contains("viagem");
	}

	@Test
	void shouldGenerateXlsx_withHeaderAndNumericAmount() throws Exception {
		TransactionExportService.ExportFile file = service.export(userId, YearMonth.of(2026, 7), null, null, null,
				null, null, "xlsx");

		assertThat(file.filename()).isEqualTo("transacoes-2026-07.xlsx");
		assertThat(file.contentType()).isEqualTo(
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

		try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file.content()))) {
			Sheet sheet = workbook.getSheetAt(0);
			Row header = sheet.getRow(0);
			assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Data");
			assertThat(header.getCell(5).getStringCellValue()).isEqualTo("Valor");

			Row dataRow = sheet.getRow(1);
			assertThat(dataRow.getCell(1).getStringCellValue()).isEqualTo("Padaria \"Sameiro\"");
			assertThat(dataRow.getCell(5).getNumericCellValue()).isEqualTo(31.73);
		}
	}

	@Test
	void shouldDefaultToCsv_whenFormatIsNotXlsx() {
		TransactionExportService.ExportFile file = service.export(userId, YearMonth.of(2026, 7), null, null, null,
				null, null, "something-else");

		assertThat(file.filename()).endsWith(".csv");
	}

	@Test
	void shouldReturnOnlyHeader_whenNoTransactionsMatch() {
		when(transactionRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

		TransactionExportService.ExportFile file = service.export(userId, YearMonth.of(2026, 7), null, null, null,
				null, null, "csv");

		String csv = new String(file.content(), java.nio.charset.StandardCharsets.UTF_8);
		assertThat(csv).isEqualTo("Data,Descrição,Conta,Categoria,Tipo,Valor,Tags,Parcela,Fatura\r\n");
	}

}
