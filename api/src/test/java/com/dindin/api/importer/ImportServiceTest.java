package com.dindin.api.importer;

import com.dindin.api.account.Account;
import com.dindin.api.account.AccountRepository;
import com.dindin.api.account.AccountType;
import com.dindin.api.category.Category;
import com.dindin.api.category.CategoryKind;
import com.dindin.api.category.CategoryRepository;
import com.dindin.api.importer.dto.AccountMappingChoice;
import com.dindin.api.importer.dto.CategoryMappingChoice;
import com.dindin.api.importer.dto.ImportCommitResponse;
import com.dindin.api.importer.dto.ImportMappingRequest;
import com.dindin.api.importer.dto.ImportPreviewResponse;
import com.dindin.api.transaction.TransactionRepository;
import com.dindin.api.transaction.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportServiceTest {

	private final UUID userId = UUID.randomUUID();
	private final UUID nubankId = UUID.randomUUID();
	private final UUID mercadoId = UUID.randomUUID();

	@Mock
	private SpreadsheetParser parser;
	@Mock
	private AccountRepository accountRepository;
	@Mock
	private CategoryRepository categoryRepository;
	@Mock
	private TransactionRepository transactionRepository;

	@InjectMocks
	private ImportService service;

	private Account nubank;
	private Category mercado;

	@BeforeEach
	void setUp() {
		nubank = new Account(userId, "Nubank", AccountType.CREDIT_CARD, 10, 20);
		ReflectionTestUtils.setField(nubank, "id", nubankId);
		mercado = new Category(userId, "Mercado", null, null, CategoryKind.EXPENSE);
		ReflectionTestUtils.setField(mercado, "id", mercadoId);
	}

	private InputStream fakeFile() {
		return new ByteArrayInputStream(new byte[0]);
	}

	private ImportRow row(String account, String category, TransactionType type, BigDecimal amount) {
		return new ImportRow("Julho", ImportSection.FIXOS, "Padaria", LocalDate.of(2026, 7, 1),
				account, category, amount, type);
	}

	@Test
	void shouldListUnmatchedAccountsAndCategoriesInPreview() throws Exception {
		when(parser.parse(any(), anyInt())).thenReturn(List.of(
				row("Nubank", "Mercado", TransactionType.EXPENSE, new BigDecimal("10.00")),
				row("Itau", "Roupa", TransactionType.EXPENSE, new BigDecimal("20.00"))));
		when(accountRepository.findAllByUserIdOrderByNameAsc(userId)).thenReturn(List.of(nubank));
		when(categoryRepository.findAllByUserIdOrderByNameAsc(userId)).thenReturn(List.of(mercado));

		ImportPreviewResponse preview = service.preview(userId, fakeFile(), 2026);

		assertThat(preview.rows()).hasSize(2);
		assertThat(preview.unmatchedAccounts()).containsExactly("Itau");
		assertThat(preview.unmatchedCategories()).containsExactly("Roupa");
	}

	@Test
	void shouldMatchAccountsAndCategoriesCaseInsensitively() throws Exception {
		when(parser.parse(any(), anyInt())).thenReturn(List.of(
				row("nubank", "mercado", TransactionType.EXPENSE, new BigDecimal("10.00"))));
		when(accountRepository.findAllByUserIdOrderByNameAsc(userId)).thenReturn(List.of(nubank));
		when(categoryRepository.findAllByUserIdOrderByNameAsc(userId)).thenReturn(List.of(mercado));

		ImportPreviewResponse preview = service.preview(userId, fakeFile(), 2026);

		assertThat(preview.unmatchedAccounts()).isEmpty();
		assertThat(preview.unmatchedCategories()).isEmpty();
	}

	@Test
	void shouldCreateMissingAccountAndCategoryOnCommit() throws Exception {
		when(parser.parse(any(), anyInt())).thenReturn(List.of(
				row("Itau", "Roupa", TransactionType.EXPENSE, new BigDecimal("50.00"))));
		when(accountRepository.findAllByUserIdOrderByNameAsc(userId)).thenReturn(List.of());
		when(categoryRepository.findAllByUserIdOrderByNameAsc(userId)).thenReturn(List.of());
		when(accountRepository.save(any())).thenAnswer(inv -> {
			Account a = inv.getArgument(0);
			ReflectionTestUtils.setField(a, "id", UUID.randomUUID());
			return a;
		});
		when(categoryRepository.save(any())).thenAnswer(inv -> {
			Category c = inv.getArgument(0);
			ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
			return c;
		});
		when(transactionRepository.existsByUserIdAndAccountIdAndDescriptionAndAmountAndDateAndType(
				any(), any(), any(), any(), any(), any())).thenReturn(false);

		ImportCommitResponse response = service.commit(userId, fakeFile(), 2026, ImportMappingRequest.empty());

		assertThat(response.accountsCreated()).isEqualTo(1);
		assertThat(response.categoriesCreated()).isEqualTo(1);
		assertThat(response.transactionsCreated()).isEqualTo(1);
		assertThat(response.transactionsSkippedAsDuplicate()).isZero();
		verify(transactionRepository).save(any());
	}

	@Test
	void shouldUseMappedExistingAccountAndCategory_whenProvided() throws Exception {
		UUID otherAccountId = UUID.randomUUID();
		UUID otherCategoryId = UUID.randomUUID();
		Account other = new Account(userId, "Conta Real", AccountType.CHECKING, null, null);
		ReflectionTestUtils.setField(other, "id", otherAccountId);
		Category otherCategory = new Category(userId, "Categoria Real", null, null, CategoryKind.EXPENSE);
		ReflectionTestUtils.setField(otherCategory, "id", otherCategoryId);

		when(parser.parse(any(), anyInt())).thenReturn(List.of(
				row("Itau", "Roupa", TransactionType.EXPENSE, new BigDecimal("50.00"))));
		when(accountRepository.findAllByUserIdOrderByNameAsc(userId)).thenReturn(List.of());
		when(categoryRepository.findAllByUserIdOrderByNameAsc(userId)).thenReturn(List.of());
		when(accountRepository.findByIdAndUserId(otherAccountId, userId)).thenReturn(java.util.Optional.of(other));
		when(categoryRepository.findByIdAndUserId(otherCategoryId, userId)).thenReturn(java.util.Optional.of(otherCategory));
		when(transactionRepository.existsByUserIdAndAccountIdAndDescriptionAndAmountAndDateAndType(
				any(), any(), any(), any(), any(), any())).thenReturn(false);

		ImportMappingRequest mapping = new ImportMappingRequest(
				Map.of("Itau", new AccountMappingChoice(otherAccountId, null)),
				Map.of("Roupa", new CategoryMappingChoice(otherCategoryId, null)));

		ImportCommitResponse response = service.commit(userId, fakeFile(), 2026, mapping);

		assertThat(response.accountsCreated()).isZero();
		assertThat(response.categoriesCreated()).isZero();
		ArgumentCaptor<com.dindin.api.transaction.Transaction> captor =
				ArgumentCaptor.forClass(com.dindin.api.transaction.Transaction.class);
		verify(transactionRepository).save(captor.capture());
		assertThat(captor.getValue().getAccountId()).isEqualTo(otherAccountId);
		assertThat(captor.getValue().getCategoryId()).isEqualTo(otherCategoryId);
	}

	@Test
	void shouldSkipDuplicateTransactionOnCommit() throws Exception {
		when(parser.parse(any(), anyInt())).thenReturn(List.of(
				row("Nubank", "Mercado", TransactionType.EXPENSE, new BigDecimal("50.00"))));
		when(accountRepository.findAllByUserIdOrderByNameAsc(userId)).thenReturn(List.of(nubank));
		when(categoryRepository.findAllByUserIdOrderByNameAsc(userId)).thenReturn(List.of(mercado));
		when(transactionRepository.existsByUserIdAndAccountIdAndDescriptionAndAmountAndDateAndType(
				userId, nubankId, "Padaria", new BigDecimal("50.00"), LocalDate.of(2026, 7, 1), TransactionType.EXPENSE))
				.thenReturn(true);

		ImportCommitResponse response = service.commit(userId, fakeFile(), 2026, ImportMappingRequest.empty());

		assertThat(response.transactionsSkippedAsDuplicate()).isEqualTo(1);
		assertThat(response.transactionsCreated()).isZero();
		verify(transactionRepository, never()).save(any());
	}

	@Test
	void shouldDefaultNewAccountToCheckingAndCategoryKindFromRowType() throws Exception {
		when(parser.parse(any(), anyInt())).thenReturn(List.of(
				row("Conta Nova", "Categoria Nova", TransactionType.INCOME, new BigDecimal("100.00"))));
		when(accountRepository.findAllByUserIdOrderByNameAsc(userId)).thenReturn(List.of());
		when(categoryRepository.findAllByUserIdOrderByNameAsc(userId)).thenReturn(List.of());
		lenient().when(transactionRepository.existsByUserIdAndAccountIdAndDescriptionAndAmountAndDateAndType(
				any(), any(), any(), any(), any(), any())).thenReturn(false);

		ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
		when(accountRepository.save(accountCaptor.capture())).thenAnswer(inv -> {
			Account a = inv.getArgument(0);
			ReflectionTestUtils.setField(a, "id", UUID.randomUUID());
			return a;
		});
		ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
		when(categoryRepository.save(categoryCaptor.capture())).thenAnswer(inv -> {
			Category c = inv.getArgument(0);
			ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
			return c;
		});

		service.commit(userId, fakeFile(), 2026, ImportMappingRequest.empty());

		assertThat(accountCaptor.getValue().getType()).isEqualTo(AccountType.CHECKING);
		assertThat(categoryCaptor.getValue().getKind()).isEqualTo(CategoryKind.INCOME);
	}

}
