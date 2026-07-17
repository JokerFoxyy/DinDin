package com.dindin.api.transaction;

import com.dindin.api.account.Account;
import com.dindin.api.account.AccountRepository;
import com.dindin.api.account.AccountType;
import com.dindin.api.category.Category;
import com.dindin.api.category.CategoryKind;
import com.dindin.api.category.CategoryRepository;
import com.dindin.api.common.error.BusinessException;
import com.dindin.api.common.error.NotFoundException;
import com.dindin.api.invoice.CardInvoice;
import com.dindin.api.invoice.CardInvoiceRepository;
import com.dindin.api.invoice.CardInvoiceService;
import com.dindin.api.transaction.dto.TransactionRequest;
import com.dindin.api.transaction.dto.TransactionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

	private final UUID userId = UUID.randomUUID();

	@Mock
	private TransactionRepository transactionRepository;

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private CategoryRepository categoryRepository;

	@Mock
	private CardInvoiceRepository cardInvoiceRepository;

	@Mock
	private CardInvoiceService cardInvoiceService;

	@InjectMocks
	private TransactionService transactionService;

	private Account checking;
	private Account creditCard;
	private Category expenseCategory;
	private Category incomeCategory;

	private void setupRefs(Account account, Category category) {
		ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(category, "id", UUID.randomUUID());
		when(accountRepository.findByIdAndUserId(account.getId(), userId)).thenReturn(Optional.of(account));
		when(categoryRepository.findByIdAndUserId(category.getId(), userId)).thenReturn(Optional.of(category));
	}

	private TransactionRequest request(Account account, Category category, TransactionType type, LocalDate date) {
		return new TransactionRequest("Padaria", new BigDecimal("31.73"), date, type,
				account.getId(), category.getId(), null);
	}

	@Test
	void shouldCreateWithoutInvoice_whenAccountIsChecking() {
		checking = new Account(userId, "Uniclass", AccountType.CHECKING, null, null);
		expenseCategory = new Category(userId, "Mercado", null, null, CategoryKind.EXPENSE);
		setupRefs(checking, expenseCategory);
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

		TransactionResponse response = transactionService.create(userId,
				request(checking, expenseCategory, TransactionType.EXPENSE, LocalDate.of(2026, 7, 9)));

		assertThat(response.invoiceMonth()).isNull();
		assertThat(response.accountName()).isEqualTo("Uniclass");
		assertThat(response.amount()).isEqualByComparingTo("31.73");
		verify(cardInvoiceService, never()).getOrCreateInvoiceFor(any(), any());
	}

	@Test
	void shouldLinkToInvoice_whenAccountIsCreditCard() {
		creditCard = new Account(userId, "Nubank", AccountType.CREDIT_CARD, 28, 7);
		expenseCategory = new Category(userId, "Mercado", null, null, CategoryKind.EXPENSE);
		setupRefs(creditCard, expenseCategory);
		CardInvoice invoice = new CardInvoice(creditCard.getId(), LocalDate.of(2026, 7, 1),
				LocalDate.of(2026, 7, 28), LocalDate.of(2026, 8, 7));
		ReflectionTestUtils.setField(invoice, "id", UUID.randomUUID());
		when(cardInvoiceService.getOrCreateInvoiceFor(eq(creditCard), any())).thenReturn(invoice);
		when(cardInvoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

		TransactionResponse response = transactionService.create(userId,
				request(creditCard, expenseCategory, TransactionType.EXPENSE, LocalDate.of(2026, 7, 9)));

		assertThat(response.invoiceMonth()).isEqualTo(LocalDate.of(2026, 7, 1));
	}

	@Test
	void shouldThrowBusiness_whenTypeIsInvoiceAdjustment() {
		assertThatThrownBy(() -> transactionService.create(userId,
				new TransactionRequest("Ajuste", BigDecimal.TEN, LocalDate.now(),
						TransactionType.INVOICE_ADJUSTMENT, UUID.randomUUID(), UUID.randomUUID(), null)))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("reservado");
		verify(transactionRepository, never()).save(any());
	}

	@Test
	void shouldThrowBusiness_whenCategoryKindDoesNotMatchType() {
		checking = new Account(userId, "Uniclass", AccountType.CHECKING, null, null);
		incomeCategory = new Category(userId, "Salário", null, null, CategoryKind.INCOME);
		setupRefs(checking, incomeCategory);

		assertThatThrownBy(() -> transactionService.create(userId,
				request(checking, incomeCategory, TransactionType.EXPENSE, LocalDate.now())))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("entrada");
	}

	@Test
	void shouldThrowNotFound_whenAccountBelongsToAnotherUser() {
		when(accountRepository.findByIdAndUserId(any(), eq(userId))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> transactionService.create(userId,
				new TransactionRequest("Padaria", BigDecimal.TEN, LocalDate.now(),
						TransactionType.EXPENSE, UUID.randomUUID(), UUID.randomUUID(), null)))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void shouldRelinkInvoice_whenUpdateMovesTransactionToCreditCard() {
		checking = new Account(userId, "Uniclass", AccountType.CHECKING, null, null);
		creditCard = new Account(userId, "Nubank", AccountType.CREDIT_CARD, 28, 7);
		expenseCategory = new Category(userId, "Mercado", null, null, CategoryKind.EXPENSE);
		setupRefs(creditCard, expenseCategory);
		Transaction transaction = new Transaction(userId, UUID.randomUUID(), UUID.randomUUID(), null,
				"Padaria", BigDecimal.TEN, LocalDate.of(2026, 7, 9), TransactionType.EXPENSE);
		when(transactionRepository.findByIdAndUserId(any(), eq(userId))).thenReturn(Optional.of(transaction));
		CardInvoice invoice = new CardInvoice(creditCard.getId(), LocalDate.of(2026, 7, 1),
				LocalDate.of(2026, 7, 28), LocalDate.of(2026, 8, 7));
		ReflectionTestUtils.setField(invoice, "id", UUID.randomUUID());
		when(cardInvoiceService.getOrCreateInvoiceFor(eq(creditCard), any())).thenReturn(invoice);
		when(cardInvoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

		TransactionResponse response = transactionService.update(userId, UUID.randomUUID(),
				request(creditCard, expenseCategory, TransactionType.EXPENSE, LocalDate.of(2026, 7, 9)));

		assertThat(response.invoiceMonth()).isEqualTo(LocalDate.of(2026, 7, 1));
		assertThat(transaction.getInvoiceId()).isEqualTo(invoice.getId());
	}

	@Test
	void shouldClearInvoice_whenUpdateMovesTransactionToCashAccount() {
		checking = new Account(userId, "Carteira", AccountType.CASH, null, null);
		expenseCategory = new Category(userId, "Mercado", null, null, CategoryKind.EXPENSE);
		setupRefs(checking, expenseCategory);
		Transaction transaction = new Transaction(userId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
				"Padaria", BigDecimal.TEN, LocalDate.of(2026, 7, 9), TransactionType.EXPENSE);
		when(transactionRepository.findByIdAndUserId(any(), eq(userId))).thenReturn(Optional.of(transaction));

		TransactionResponse response = transactionService.update(userId, UUID.randomUUID(),
				request(checking, expenseCategory, TransactionType.EXPENSE, LocalDate.of(2026, 7, 9)));

		assertThat(response.invoiceMonth()).isNull();
		assertThat(transaction.getInvoiceId()).isNull();
	}

	@Test
	void shouldThrowBusiness_whenEditingInvoiceAdjustment() {
		Transaction adjustment = new Transaction(userId, UUID.randomUUID(), null, UUID.randomUUID(),
				"Diferença de totais", BigDecimal.TEN, LocalDate.now(), TransactionType.INVOICE_ADJUSTMENT);
		when(transactionRepository.findByIdAndUserId(any(), eq(userId))).thenReturn(Optional.of(adjustment));

		assertThatThrownBy(() -> transactionService.update(userId, UUID.randomUUID(),
				new TransactionRequest("x", BigDecimal.ONE, LocalDate.now(),
						TransactionType.EXPENSE, UUID.randomUUID(), UUID.randomUUID(), null)))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	void shouldDeleteTransaction_whenOwnedByUser() {
		Transaction transaction = new Transaction(userId, UUID.randomUUID(), UUID.randomUUID(), null,
				"Padaria", BigDecimal.TEN, LocalDate.now(), TransactionType.EXPENSE);
		when(transactionRepository.findByIdAndUserId(any(), eq(userId))).thenReturn(Optional.of(transaction));

		transactionService.delete(userId, UUID.randomUUID());

		verify(transactionRepository).delete(transaction);
	}

	@Test
	void shouldThrowNotFound_whenDeletingTransactionOfAnotherUser() {
		when(transactionRepository.findByIdAndUserId(any(), eq(userId))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> transactionService.delete(userId, UUID.randomUUID()))
				.isInstanceOf(NotFoundException.class);
	}

}
