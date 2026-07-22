package com.guaranin.api.invoice;

import com.guaranin.api.account.Account;
import com.guaranin.api.account.AccountType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardInvoiceServiceTest {

	@Mock
	private CardInvoiceRepository cardInvoiceRepository;

	@InjectMocks
	private CardInvoiceService cardInvoiceService;

	private Account card(int closingDay, int dueDay) {
		return new Account(UUID.randomUUID(), "Nubank", AccountType.CREDIT_CARD, closingDay, dueDay);
	}

	@Test
	void shouldUseSameMonth_whenPurchaseIsBeforeClosingDay() {
		assertThat(CardInvoiceService.invoiceMonthFor(28, LocalDate.of(2026, 7, 27)))
				.isEqualTo(YearMonth.of(2026, 7));
	}

	@Test
	void shouldUseNextMonth_whenPurchaseIsOnClosingDay() {
		assertThat(CardInvoiceService.invoiceMonthFor(28, LocalDate.of(2026, 7, 28)))
				.isEqualTo(YearMonth.of(2026, 8));
	}

	@Test
	void shouldUseNextMonth_whenPurchaseIsAfterClosingDay() {
		assertThat(CardInvoiceService.invoiceMonthFor(28, LocalDate.of(2026, 12, 30)))
				.isEqualTo(YearMonth.of(2027, 1));
	}

	@Test
	void shouldClampClosingDay_whenMonthIsShorterThanClosingDay() {
		// fechamento dia 31 em fevereiro: fecha no dia 28 → compra do dia 28 vai para março
		assertThat(CardInvoiceService.invoiceMonthFor(31, LocalDate.of(2026, 2, 28)))
				.isEqualTo(YearMonth.of(2026, 3));
		assertThat(CardInvoiceService.invoiceMonthFor(31, LocalDate.of(2026, 2, 27)))
				.isEqualTo(YearMonth.of(2026, 2));
	}

	@Test
	void shouldCreateInvoiceWithDueDateInSameMonth_whenDueDayIsAfterClosingDay() {
		when(cardInvoiceRepository.findByAccountIdAndMonth(any(), any())).thenReturn(Optional.empty());
		when(cardInvoiceRepository.save(any(CardInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

		CardInvoice invoice = cardInvoiceService.getOrCreateInvoiceFor(card(10, 20), LocalDate.of(2026, 7, 5));

		assertThat(invoice.getMonth()).isEqualTo(LocalDate.of(2026, 7, 1));
		assertThat(invoice.getClosingDate()).isEqualTo(LocalDate.of(2026, 7, 10));
		assertThat(invoice.getDueDate()).isEqualTo(LocalDate.of(2026, 7, 20));
		assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.OPEN);
	}

	@Test
	void shouldCreateInvoiceWithDueDateInNextMonth_whenDueDayIsBeforeClosingDay() {
		when(cardInvoiceRepository.findByAccountIdAndMonth(any(), any())).thenReturn(Optional.empty());
		when(cardInvoiceRepository.save(any(CardInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

		CardInvoice invoice = cardInvoiceService.getOrCreateInvoiceFor(card(28, 7), LocalDate.of(2026, 7, 10));

		assertThat(invoice.getMonth()).isEqualTo(LocalDate.of(2026, 7, 1));
		assertThat(invoice.getClosingDate()).isEqualTo(LocalDate.of(2026, 7, 28));
		assertThat(invoice.getDueDate()).isEqualTo(LocalDate.of(2026, 8, 7));
	}

	@Test
	void shouldClampClosingDateAndPushDueToNextMonth_whenInvoiceMonthIsFebruary() {
		when(cardInvoiceRepository.findByAccountIdAndMonth(any(), any())).thenReturn(Optional.empty());
		when(cardInvoiceRepository.save(any(CardInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

		// fechamento 31 clampa para 28/02; vencimento 30 não vem depois do fechamento
		// dentro do mês, então cai no mês seguinte
		CardInvoice invoice = cardInvoiceService.getOrCreateInvoiceFor(card(31, 30), LocalDate.of(2026, 2, 10));

		assertThat(invoice.getMonth()).isEqualTo(LocalDate.of(2026, 2, 1));
		assertThat(invoice.getClosingDate()).isEqualTo(LocalDate.of(2026, 2, 28));
		assertThat(invoice.getDueDate()).isEqualTo(LocalDate.of(2026, 3, 30));
	}

	@Test
	void shouldReuseExistingInvoice_whenPeriodAlreadyHasOne() {
		Account card = card(28, 7);
		CardInvoice existing = new CardInvoice(card.getId(), LocalDate.of(2026, 7, 1),
				LocalDate.of(2026, 7, 28), LocalDate.of(2026, 8, 7));
		when(cardInvoiceRepository.findByAccountIdAndMonth(card.getId(), LocalDate.of(2026, 7, 1)))
				.thenReturn(Optional.of(existing));

		CardInvoice invoice = cardInvoiceService.getOrCreateInvoiceFor(card, LocalDate.of(2026, 7, 10));

		assertThat(invoice).isSameAs(existing);
		verify(cardInvoiceRepository, never()).save(any());
	}

	@Test
	void shouldReportMonthCaptured_whenNewInvoiceIsSaved() {
		when(cardInvoiceRepository.findByAccountIdAndMonth(any(), any())).thenReturn(Optional.empty());
		when(cardInvoiceRepository.save(any(CardInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

		cardInvoiceService.getOrCreateInvoiceFor(card(28, 7), LocalDate.of(2026, 7, 30));

		ArgumentCaptor<CardInvoice> saved = ArgumentCaptor.forClass(CardInvoice.class);
		verify(cardInvoiceRepository).save(saved.capture());
		assertThat(saved.getValue().getMonth()).isEqualTo(LocalDate.of(2026, 8, 1));
	}

}
