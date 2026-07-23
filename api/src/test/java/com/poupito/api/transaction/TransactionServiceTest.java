package com.poupito.api.transaction;

import com.poupito.api.account.Account;
import com.poupito.api.account.AccountRepository;
import com.poupito.api.account.AccountType;
import com.poupito.api.card.Card;
import com.poupito.api.card.CardRepository;
import com.poupito.api.category.Category;
import com.poupito.api.category.CategoryKind;
import com.poupito.api.category.CategoryRepository;
import com.poupito.api.common.error.BusinessException;
import com.poupito.api.common.error.NotFoundException;
import com.poupito.api.invoice.CardInvoice;
import com.poupito.api.invoice.CardInvoiceRepository;
import com.poupito.api.invoice.CardInvoiceService;
import com.poupito.api.transaction.dto.TransactionRequest;
import com.poupito.api.transaction.dto.TransactionResponse;
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
	private CardRepository cardRepository;
	@Mock
	private CategoryRepository categoryRepository;
	@Mock
	private CardInvoiceRepository cardInvoiceRepository;
	@Mock
	private CardInvoiceService cardInvoiceService;

	@InjectMocks
	private TransactionService transactionService;

	private Account account(AccountType type) {
		Account account = new Account(userId, type == AccountType.CASH ? "Carteira" : "Uniclass", type);
		ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
		org.mockito.Mockito.lenient()
				.when(accountRepository.findByIdAndUserId(account.getId(), userId)).thenReturn(Optional.of(account));
		return account;
	}

	private Card card() {
		Card card = new Card(userId, UUID.randomUUID(), "Nubank", 28, 7);
		ReflectionTestUtils.setField(card, "id", UUID.randomUUID());
		org.mockito.Mockito.lenient()
				.when(cardRepository.findByIdAndUserId(card.getId(), userId)).thenReturn(Optional.of(card));
		return card;
	}

	private Category category(CategoryKind kind) {
		Category category = new Category(userId, kind == CategoryKind.INCOME ? "Salário" : "Mercado",
				null, null, kind);
		ReflectionTestUtils.setField(category, "id", UUID.randomUUID());
		org.mockito.Mockito.lenient()
				.when(categoryRepository.findByIdAndUserId(category.getId(), userId)).thenReturn(Optional.of(category));
		return category;
	}

	private CardInvoice mockInvoiceFor(Card card) {
		CardInvoice invoice = new CardInvoice(card.getId(), LocalDate.of(2026, 7, 1),
				LocalDate.of(2026, 7, 28), LocalDate.of(2026, 8, 7));
		ReflectionTestUtils.setField(invoice, "id", UUID.randomUUID());
		when(cardInvoiceService.getOrCreateInvoiceFor(eq(card), any())).thenReturn(invoice);
		when(cardInvoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
		return invoice;
	}

	private TransactionRequest accountRequest(Account account, Category category, TransactionType type, LocalDate date) {
		return new TransactionRequest("Padaria", new BigDecimal("31.73"), date, type,
				account.getId(), null, category.getId(), null, null);
	}

	private TransactionRequest cardRequest(Card card, Category category, TransactionType type, LocalDate date,
			Integer installments) {
		return new TransactionRequest("Compra", new BigDecimal("500.00"), date, type,
				null, card.getId(), category.getId(), null, installments);
	}

	@Test
	void shouldCreateWithoutInvoice_whenPaidWithAccount() {
		Account checking = account(AccountType.CHECKING);
		Category expense = category(CategoryKind.EXPENSE);
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

		TransactionResponse response = transactionService.create(userId,
				accountRequest(checking, expense, TransactionType.EXPENSE, LocalDate.of(2026, 7, 9)));

		assertThat(response.invoiceMonth()).isNull();
		assertThat(response.accountName()).isEqualTo("Uniclass");
		assertThat(response.method()).isEqualTo(PaymentMethod.DEBITO);
		verify(cardInvoiceService, never()).getOrCreateInvoiceFor(any(), any());
	}

	@Test
	void shouldReportCashMethod_whenAccountIsCash() {
		Account cash = account(AccountType.CASH);
		Category expense = category(CategoryKind.EXPENSE);
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

		TransactionResponse response = transactionService.create(userId,
				accountRequest(cash, expense, TransactionType.EXPENSE, LocalDate.of(2026, 7, 9)));

		assertThat(response.method()).isEqualTo(PaymentMethod.DINHEIRO);
	}

	@Test
	void shouldLinkToInvoice_whenPaidWithCard() {
		Card card = card();
		Category expense = category(CategoryKind.EXPENSE);
		mockInvoiceFor(card);
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

		TransactionResponse response = transactionService.create(userId,
				cardRequest(card, expense, TransactionType.EXPENSE, LocalDate.of(2026, 7, 9), null));

		assertThat(response.invoiceMonth()).isEqualTo(LocalDate.of(2026, 7, 1));
		assertThat(response.method()).isEqualTo(PaymentMethod.CREDITO);
		assertThat(response.cardName()).isEqualTo("Nubank");
	}

	@Test
	void shouldThrowBusiness_whenNeitherAccountNorCardProvided() {
		Category expense = category(CategoryKind.EXPENSE);

		assertThatThrownBy(() -> transactionService.create(userId,
				new TransactionRequest("X", BigDecimal.TEN, LocalDate.now(), TransactionType.EXPENSE,
						null, null, expense.getId(), null, null)))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("conta OU cartão");
	}

	@Test
	void shouldThrowBusiness_whenBothAccountAndCardProvided() {
		assertThatThrownBy(() -> transactionService.create(userId,
				new TransactionRequest("X", BigDecimal.TEN, LocalDate.now(), TransactionType.EXPENSE,
						UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, null)))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("conta OU cartão");
	}

	@Test
	void shouldThrowBusiness_whenIncomeIsPaidWithCard() {
		Card card = card();

		assertThatThrownBy(() -> transactionService.create(userId,
				new TransactionRequest("Salário", BigDecimal.TEN, LocalDate.now(), TransactionType.INCOME,
						null, card.getId(), UUID.randomUUID(), null, null)))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("cartão");
	}

	@Test
	void shouldThrowBusiness_whenTypeIsReserved() {
		assertThatThrownBy(() -> transactionService.create(userId,
				new TransactionRequest("Ajuste", BigDecimal.TEN, LocalDate.now(),
						TransactionType.INVOICE_ADJUSTMENT, UUID.randomUUID(), null, UUID.randomUUID(), null, null)))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("reservado");
		verify(transactionRepository, never()).save(any());
	}

	@Test
	void shouldThrowBusiness_whenCategoryKindDoesNotMatchType() {
		Account checking = account(AccountType.CHECKING);
		Category income = category(CategoryKind.INCOME);

		assertThatThrownBy(() -> transactionService.create(userId,
				accountRequest(checking, income, TransactionType.EXPENSE, LocalDate.now())))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("entrada");
	}

	@Test
	void shouldThrowNotFound_whenAccountBelongsToAnotherUser() {
		when(accountRepository.findByIdAndUserId(any(), eq(userId))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> transactionService.create(userId,
				new TransactionRequest("Padaria", BigDecimal.TEN, LocalDate.now(),
						TransactionType.EXPENSE, UUID.randomUUID(), null, UUID.randomUUID(), null, null)))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void shouldMoveTransactionFromAccountToCard_onUpdate() {
		Card card = card();
		Category expense = category(CategoryKind.EXPENSE);
		Transaction transaction = Transaction.forAccount(userId, UUID.randomUUID(), UUID.randomUUID(),
				"Padaria", BigDecimal.TEN, LocalDate.of(2026, 7, 9), TransactionType.EXPENSE);
		when(transactionRepository.findByIdAndUserId(any(), eq(userId))).thenReturn(Optional.of(transaction));
		mockInvoiceFor(card);

		TransactionResponse response = transactionService.update(userId, UUID.randomUUID(),
				cardRequest(card, expense, TransactionType.EXPENSE, LocalDate.of(2026, 7, 9), null));

		assertThat(response.invoiceMonth()).isEqualTo(LocalDate.of(2026, 7, 1));
		assertThat(transaction.getCardId()).isEqualTo(card.getId());
		assertThat(transaction.getAccountId()).isNull();
	}

	@Test
	void shouldMoveTransactionFromCardToAccount_onUpdate() {
		Account checking = account(AccountType.CHECKING);
		Category expense = category(CategoryKind.EXPENSE);
		Transaction transaction = Transaction.forCard(userId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
				"Padaria", BigDecimal.TEN, LocalDate.of(2026, 7, 9), TransactionType.EXPENSE);
		when(transactionRepository.findByIdAndUserId(any(), eq(userId))).thenReturn(Optional.of(transaction));

		TransactionResponse response = transactionService.update(userId, UUID.randomUUID(),
				accountRequest(checking, expense, TransactionType.EXPENSE, LocalDate.of(2026, 7, 9)));

		assertThat(response.invoiceMonth()).isNull();
		assertThat(transaction.getInvoiceId()).isNull();
		assertThat(transaction.getAccountId()).isEqualTo(checking.getId());
		assertThat(transaction.getCardId()).isNull();
	}

	@Test
	void shouldThrowBusiness_whenEditingInvoiceAdjustment() {
		Transaction adjustment = Transaction.forCard(userId, UUID.randomUUID(), null, UUID.randomUUID(),
				"Ajuste de fatura", BigDecimal.TEN, LocalDate.now(), TransactionType.INVOICE_ADJUSTMENT);
		when(transactionRepository.findByIdAndUserId(any(), eq(userId))).thenReturn(Optional.of(adjustment));

		assertThatThrownBy(() -> transactionService.update(userId, UUID.randomUUID(),
				new TransactionRequest("x", BigDecimal.ONE, LocalDate.now(),
						TransactionType.EXPENSE, UUID.randomUUID(), null, UUID.randomUUID(), null, null)))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	void shouldThrowBusiness_whenEditingInvoicePayment() {
		Transaction payment = Transaction.forAccount(userId, UUID.randomUUID(), null,
				"Pagamento fatura", BigDecimal.TEN, LocalDate.now(), TransactionType.INVOICE_PAYMENT);
		when(transactionRepository.findByIdAndUserId(any(), eq(userId))).thenReturn(Optional.of(payment));

		assertThatThrownBy(() -> transactionService.update(userId, UUID.randomUUID(),
				new TransactionRequest("x", BigDecimal.ONE, LocalDate.now(),
						TransactionType.EXPENSE, UUID.randomUUID(), null, UUID.randomUUID(), null, null)))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	void shouldDeleteTransaction_whenOwnedByUser() {
		Transaction transaction = Transaction.forAccount(userId, UUID.randomUUID(), UUID.randomUUID(),
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
	void shouldCreateNInstallmentsOnCard_withConsecutiveMonthlyInvoices() {
		Card card = card();
		Category expense = category(CategoryKind.EXPENSE);
		mockInvoiceFor(card);
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

		TransactionResponse response = transactionService.create(userId,
				cardRequest(card, expense, TransactionType.EXPENSE, LocalDate.of(2026, 7, 9), 2));

		verify(transactionRepository, org.mockito.Mockito.times(2)).save(any(Transaction.class));
		verify(cardInvoiceService).getOrCreateInvoiceFor(card, LocalDate.of(2026, 7, 9));
		verify(cardInvoiceService).getOrCreateInvoiceFor(card, LocalDate.of(2026, 8, 9));
		assertThat(response.installmentNumber()).isEqualTo(1);
		assertThat(response.installmentCount()).isEqualTo(2);
	}

	@Test
	void shouldThrowBusiness_whenInstallmentsRequestedForAccount() {
		Account checking = account(AccountType.CHECKING);
		Category expense = category(CategoryKind.EXPENSE);

		TransactionRequest request = new TransactionRequest("Notebook", BigDecimal.TEN, LocalDate.now(),
				TransactionType.EXPENSE, checking.getId(), null, expense.getId(), null, 3);

		assertThatThrownBy(() -> transactionService.create(userId, request))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("cartão");
		verify(transactionRepository, never()).save(any());
	}

	@Test
	void shouldThrowBusiness_whenInstallmentsRequestedForIncome() {
		Card card = card();
		Category income = category(CategoryKind.INCOME);

		TransactionRequest request = new TransactionRequest("Salário", BigDecimal.TEN, LocalDate.now(),
				TransactionType.INCOME, null, card.getId(), income.getId(), null, 3);

		assertThatThrownBy(() -> transactionService.create(userId, request))
				.isInstanceOf(BusinessException.class);
		verify(transactionRepository, never()).save(any());
	}

	@Test
	void shouldDeleteOnlyFutureInstallmentsOfTheGroup_whenScopeIsGroup() {
		UUID groupId = UUID.randomUUID();
		Transaction clicked = Transaction.installment(userId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
				"Notebook", BigDecimal.TEN, LocalDate.of(2026, 3, 1), TransactionType.EXPENSE, groupId, 2, 6);
		when(transactionRepository.findByIdAndUserId(any(), eq(userId))).thenReturn(Optional.of(clicked));
		when(transactionRepository.findAllByUserIdAndInstallmentGroupIdAndDateGreaterThanEqual(
				userId, groupId, LocalDate.of(2026, 3, 1))).thenReturn(List.of(clicked));

		transactionService.delete(userId, UUID.randomUUID(), "group");

		verify(transactionRepository).deleteAll(List.of(clicked));
		verify(transactionRepository, never()).delete(any(Transaction.class));
	}

}
