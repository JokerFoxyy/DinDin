package com.guaranin.api.transaction;

import com.guaranin.api.account.Account;
import com.guaranin.api.account.AccountRepository;
import com.guaranin.api.account.AccountType;
import com.guaranin.api.category.Category;
import com.guaranin.api.category.CategoryKind;
import com.guaranin.api.category.CategoryRepository;
import com.guaranin.api.common.error.BusinessException;
import com.guaranin.api.common.error.NotFoundException;
import com.guaranin.api.invoice.CardInvoice;
import com.guaranin.api.invoice.CardInvoiceRepository;
import com.guaranin.api.invoice.CardInvoiceService;
import com.guaranin.api.transaction.dto.TransactionRequest;
import com.guaranin.api.transaction.dto.TransactionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
				account.getId(), category.getId(), null, null);
	}

	private TransactionRequest requestWithInstallments(Account account, Category category, LocalDate date,
			int installments) {
		return new TransactionRequest("Notebook", new BigDecimal("500.00"), date, TransactionType.EXPENSE,
				account.getId(), category.getId(), null, installments);
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
						TransactionType.INVOICE_ADJUSTMENT, UUID.randomUUID(), UUID.randomUUID(), null, null)))
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
						TransactionType.EXPENSE, UUID.randomUUID(), UUID.randomUUID(), null, null)))
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
						TransactionType.EXPENSE, UUID.randomUUID(), UUID.randomUUID(), null, null)))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	void shouldDeleteTransaction_whenOwnedByUser() {
		Transaction transaction = new Transaction(userId, UUID.randomUUID(), UUID.randomUUID(), null,
				"Padaria", BigDecimal.TEN, LocalDate.now(), TransactionType.EXPENSE);
		when(transactionRepository.findByIdAndUserId(any(), eq(userId))).thenReturn(Optional.of(transaction));

		transactionService.delete(userId, UUID.randomUUID(), null);

		verify(transactionRepository).delete(transaction);
	}

	@Test
	void shouldThrowNotFound_whenDeletingTransactionOfAnotherUser() {
		when(transactionRepository.findByIdAndUserId(any(), eq(userId))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> transactionService.delete(userId, UUID.randomUUID(), null))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void shouldCreateNInstallments_withConsecutiveMonthlyDatesAndSameAmount() {
		checking = new Account(userId, "Uniclass", AccountType.CHECKING, null, null);
		expenseCategory = new Category(userId, "Eletrônicos", null, null, CategoryKind.EXPENSE);
		setupRefs(checking, expenseCategory);
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

		TransactionResponse response = transactionService.create(userId,
				requestWithInstallments(checking, expenseCategory, LocalDate.of(2026, 1, 31), 3));

		verify(transactionRepository, org.mockito.Mockito.times(3)).save(any(Transaction.class));
		assertThat(response.installmentNumber()).isEqualTo(1);
		assertThat(response.installmentCount()).isEqualTo(3);
		assertThat(response.date()).isEqualTo(LocalDate.of(2026, 1, 31));
	}

	@Test
	void shouldLinkEachInstallmentToItsOwnMonthlyInvoice_whenAccountIsCreditCard() {
		creditCard = new Account(userId, "Nubank", AccountType.CREDIT_CARD, 28, 7);
		expenseCategory = new Category(userId, "Eletrônicos", null, null, CategoryKind.EXPENSE);
		setupRefs(creditCard, expenseCategory);
		CardInvoice invoice = new CardInvoice(creditCard.getId(), LocalDate.of(2026, 7, 1),
				LocalDate.of(2026, 7, 28), LocalDate.of(2026, 8, 7));
		ReflectionTestUtils.setField(invoice, "id", UUID.randomUUID());
		when(cardInvoiceService.getOrCreateInvoiceFor(eq(creditCard), any())).thenReturn(invoice);
		when(cardInvoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

		transactionService.create(userId,
				requestWithInstallments(creditCard, expenseCategory, LocalDate.of(2026, 7, 9), 2));

		verify(cardInvoiceService).getOrCreateInvoiceFor(creditCard, LocalDate.of(2026, 7, 9));
		verify(cardInvoiceService).getOrCreateInvoiceFor(creditCard, LocalDate.of(2026, 8, 9));
	}

	@Test
	void shouldThrowBusiness_whenInstallmentsRequestedForIncome() {
		checking = new Account(userId, "Uniclass", AccountType.CHECKING, null, null);
		incomeCategory = new Category(userId, "Salário", null, null, CategoryKind.INCOME);
		setupRefs(checking, incomeCategory);

		TransactionRequest request = new TransactionRequest("Salário", BigDecimal.TEN, LocalDate.now(),
				TransactionType.INCOME, checking.getId(), incomeCategory.getId(), null, 3);

		assertThatThrownBy(() -> transactionService.create(userId, request))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("gastos");
		verify(transactionRepository, never()).save(any());
	}

	@Test
	void shouldDeleteOnlyFutureInstallmentsOfTheGroup_whenScopeIsGroup() {
		UUID groupId = UUID.randomUUID();
		Transaction clicked = Transaction.installment(userId, UUID.randomUUID(), UUID.randomUUID(), null,
				"Notebook", BigDecimal.TEN, LocalDate.of(2026, 3, 1), TransactionType.EXPENSE, groupId, 2, 6);
		when(transactionRepository.findByIdAndUserId(any(), eq(userId))).thenReturn(Optional.of(clicked));
		when(transactionRepository.findAllByUserIdAndInstallmentGroupIdAndDateGreaterThanEqual(
				userId, groupId, LocalDate.of(2026, 3, 1))).thenReturn(List.of(clicked));

		transactionService.delete(userId, UUID.randomUUID(), "group");

		verify(transactionRepository).deleteAll(List.of(clicked));
		verify(transactionRepository, never()).delete(any(Transaction.class));
	}

	@Test
	void shouldDeleteOnlyThisTransaction_whenScopeIsGroupButItIsNotAnInstallment() {
		Transaction transaction = new Transaction(userId, UUID.randomUUID(), UUID.randomUUID(), null,
				"Padaria", BigDecimal.TEN, LocalDate.now(), TransactionType.EXPENSE);
		when(transactionRepository.findByIdAndUserId(any(), eq(userId))).thenReturn(Optional.of(transaction));

		transactionService.delete(userId, UUID.randomUUID(), "group");

		verify(transactionRepository).delete(transaction);
	}

}
