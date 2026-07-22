package com.poupito.api.invoice;

import com.poupito.api.account.Account;
import com.poupito.api.account.AccountRepository;
import com.poupito.api.account.AccountType;
import com.poupito.api.category.CategoryRepository;
import com.poupito.api.common.error.BusinessException;
import com.poupito.api.common.error.NotFoundException;
import com.poupito.api.invoice.dto.InvoiceDetailResponse;
import com.poupito.api.transaction.Transaction;
import com.poupito.api.transaction.TransactionRepository;
import com.poupito.api.transaction.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

	private final UUID userId = UUID.randomUUID();
	private final UUID accountId = UUID.randomUUID();
	private final UUID invoiceId = UUID.randomUUID();

	@Mock
	private CardInvoiceRepository cardInvoiceRepository;
	@Mock
	private TransactionRepository transactionRepository;
	@Mock
	private AccountRepository accountRepository;
	@Mock
	private CategoryRepository categoryRepository;

	@InjectMocks
	private InvoiceService service;

	private Account card;
	private CardInvoice invoice;

	@BeforeEach
	void setUp() {
		card = new Account(userId, "Nubank", AccountType.CREDIT_CARD, 28, 7);
		ReflectionTestUtils.setField(card, "id", accountId);
		invoice = new CardInvoice(accountId, LocalDate.of(2026, 7, 1),
				LocalDate.of(2026, 7, 28), LocalDate.of(2026, 8, 7));
		ReflectionTestUtils.setField(invoice, "id", invoiceId);
		lenient().when(cardInvoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
		lenient().when(accountRepository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.of(card));
		lenient().when(categoryRepository.findAllById(anyIterable())).thenReturn(List.of());
	}

	private Transaction purchase(String amount) {
		return new Transaction(userId, accountId, null, invoiceId, "Compra",
				new BigDecimal(amount), LocalDate.of(2026, 7, 15), TransactionType.EXPENSE);
	}

	private Transaction adjustment(String amount) {
		return new Transaction(userId, accountId, null, invoiceId, "Ajuste de fatura",
				new BigDecimal(amount), LocalDate.of(2026, 7, 28), TransactionType.INVOICE_ADJUSTMENT);
	}

	@Test
	void shouldCreateAdjustment_whenClosingWithHigherDeclaredTotal() {
		when(transactionRepository.findAllByInvoiceIdOrderByDateAsc(invoiceId)).thenReturn(List.of(purchase("100.00")));

		service.close(userId, invoiceId, new BigDecimal("150.00"));

		assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.CLOSED);
		ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
		verify(transactionRepository).save(saved.capture());
		assertThat(saved.getValue().getType()).isEqualTo(TransactionType.INVOICE_ADJUSTMENT);
		assertThat(saved.getValue().getAmount()).isEqualByComparingTo("50.00");
	}

	@Test
	void shouldNotCreateAdjustment_whenLaunchedMeetsDeclared() {
		when(transactionRepository.findAllByInvoiceIdOrderByDateAsc(invoiceId)).thenReturn(List.of(purchase("150.00")));

		service.close(userId, invoiceId, new BigDecimal("150.00"));

		verify(transactionRepository, never()).save(any());
	}

	@Test
	void shouldReduceAdjustment_whenDetailingRealTransactions() {
		Transaction adjustment = adjustment("50.00");
		ReflectionTestUtils.setField(invoice, "declaredTotal", new BigDecimal("150.00"));
		ReflectionTestUtils.setField(invoice, "status", InvoiceStatus.CLOSED);
		// usuário detalhou parte: 100 + 20 lançados, ajuste antigo de 50
		when(transactionRepository.findAllByInvoiceIdOrderByDateAsc(invoiceId))
				.thenReturn(List.of(purchase("100.00"), purchase("20.00"), adjustment));

		service.getDetail(userId, invoiceId);

		assertThat(adjustment.getAmount()).isEqualByComparingTo("30.00");
	}

	@Test
	void shouldRemoveAdjustment_whenFullyDetailed() {
		Transaction adjustment = adjustment("50.00");
		ReflectionTestUtils.setField(invoice, "declaredTotal", new BigDecimal("150.00"));
		ReflectionTestUtils.setField(invoice, "status", InvoiceStatus.CLOSED);
		when(transactionRepository.findAllByInvoiceIdOrderByDateAsc(invoiceId))
				.thenReturn(List.of(purchase("100.00"), purchase("50.00"), adjustment));

		service.getDetail(userId, invoiceId);

		verify(transactionRepository).delete(adjustment);
	}

	@Test
	void shouldPayClosedInvoice() {
		ReflectionTestUtils.setField(invoice, "status", InvoiceStatus.CLOSED);
		when(transactionRepository.findAllByInvoiceIdOrderByDateAsc(invoiceId)).thenReturn(List.of());

		InvoiceDetailResponse detail = service.pay(userId, invoiceId);

		assertThat(detail.invoice().status()).isEqualTo(InvoiceStatus.PAID);
	}

	@Test
	void shouldThrowBusiness_whenPayingOpenInvoice() {
		assertThatThrownBy(() -> service.pay(userId, invoiceId)).isInstanceOf(BusinessException.class);
	}

	@Test
	void shouldThrowBusiness_whenClosingAlreadyClosedInvoice() {
		ReflectionTestUtils.setField(invoice, "status", InvoiceStatus.CLOSED);

		assertThatThrownBy(() -> service.close(userId, invoiceId, new BigDecimal("10.00")))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	void shouldThrowBusiness_whenReopeningOpenInvoice() {
		assertThatThrownBy(() -> service.reopen(userId, invoiceId)).isInstanceOf(BusinessException.class);
	}

	@Test
	void shouldReopenClosedInvoice() {
		ReflectionTestUtils.setField(invoice, "status", InvoiceStatus.CLOSED);
		when(transactionRepository.findAllByInvoiceIdOrderByDateAsc(invoiceId)).thenReturn(List.of());

		InvoiceDetailResponse detail = service.reopen(userId, invoiceId);

		assertThat(detail.invoice().status()).isEqualTo(InvoiceStatus.OPEN);
	}

	@Test
	void shouldThrowNotFound_whenInvoiceBelongsToAnotherUser() {
		when(accountRepository.findByIdAndUserId(accountId, userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getDetail(userId, invoiceId)).isInstanceOf(NotFoundException.class);
	}

	@Test
	void shouldListInvoicesForMonthWithLaunchedTotal() {
		when(accountRepository.findAllByUserIdOrderByNameAsc(userId)).thenReturn(List.of(card));
		when(cardInvoiceRepository.findByAccountIdInAndMonthOrderByCreatedAtAsc(any(), any()))
				.thenReturn(List.of(invoice));
		when(transactionRepository.findAllByInvoiceIdOrderByDateAsc(invoiceId))
				.thenReturn(List.of(purchase("100.00"), adjustment("50.00")));

		var invoices = service.list(userId, java.time.YearMonth.of(2026, 7));

		assertThat(invoices).hasSize(1);
		assertThat(invoices.getFirst().launchedTotal()).isEqualByComparingTo("100.00");
		assertThat(invoices.getFirst().adjustment()).isEqualByComparingTo("50.00");
	}

	@Test
	void shouldReconcileAdjustment_whenListingClosedInvoice() {
		ReflectionTestUtils.setField(invoice, "status", InvoiceStatus.CLOSED);
		ReflectionTestUtils.setField(invoice, "declaredTotal", new BigDecimal("150.00"));
		Transaction adjustment = adjustment("50.00");
		when(accountRepository.findAllByUserIdOrderByNameAsc(userId)).thenReturn(List.of(card));
		when(cardInvoiceRepository.findByAccountIdInAndMonthOrderByCreatedAtAsc(any(), any()))
				.thenReturn(List.of(invoice));
		// já detalhou parte (100 + 30 lançados); a listagem deve reconciliar o ajuste para 20
		when(transactionRepository.findAllByInvoiceIdOrderByDateAsc(invoiceId))
				.thenReturn(List.of(purchase("100.00"), purchase("30.00"), adjustment));

		var invoices = service.list(userId, java.time.YearMonth.of(2026, 7));

		assertThat(adjustment.getAmount()).isEqualByComparingTo("20.00");
		assertThat(invoices.getFirst().adjustment()).isEqualByComparingTo("20.00");
	}

	@Test
	void shouldReturnEmpty_whenUserHasNoCreditCards() {
		Account checking = new Account(userId, "Uniclass", AccountType.CHECKING, null, null);
		when(accountRepository.findAllByUserIdOrderByNameAsc(userId)).thenReturn(List.of(checking));

		assertThat(service.list(userId, java.time.YearMonth.of(2026, 7))).isEmpty();
	}

}
