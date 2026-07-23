package com.poupito.api.recurring;

import com.poupito.api.account.Account;
import com.poupito.api.account.AccountRepository;
import com.poupito.api.account.AccountType;
import com.poupito.api.category.CategoryRepository;
import com.poupito.api.recurring.dto.OccurrenceResponse;
import com.poupito.api.transaction.Transaction;
import com.poupito.api.transaction.TransactionRepository;
import com.poupito.api.transaction.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringMaterializationServiceTest {

	private final UUID userId = UUID.randomUUID();

	@Mock
	private RecurringTransactionRepository recurringRepository;
	@Mock
	private TransactionRepository transactionRepository;
	@Mock
	private AccountRepository accountRepository;
	@Mock
	private CategoryRepository categoryRepository;

	@InjectMocks
	private RecurringMaterializationService service;

	private RecurringTransaction recurring(int day, LocalDate endDate, boolean active) {
		Account account = new Account(userId, "Conta", AccountType.CHECKING);
		UUID accountId = UUID.randomUUID();
		ReflectionTestUtils.setField(account, "id", accountId);
		org.mockito.Mockito.lenient().when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
		RecurringTransaction recurring = new RecurringTransaction(userId, accountId, UUID.randomUUID(),
				"Spotify", new BigDecimal("27.90"), TransactionType.EXPENSE, day, active, endDate);
		ReflectionTestUtils.setField(recurring, "id", UUID.randomUUID());
		return recurring;
	}

	@Test
	void shouldMaterializeOccurrence_whenNoneExistsForTheMonth() {
		RecurringTransaction recurring = recurring(10, null, true);
		when(transactionRepository.findByRecurringIdAndDateBetween(eq(recurring.getId()), any(), any()))
				.thenReturn(Optional.empty());
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

		Transaction created = service.materializeOccurrence(recurring, YearMonth.of(2026, 7));

		assertThat(created.getDate()).isEqualTo(LocalDate.of(2026, 7, 10));
		assertThat(created.isPaid()).isFalse();
		assertThat(created.getAccountId()).isEqualTo(recurring.getAccountId());
		assertThat(created.getCardId()).isNull();
		assertThat(created.getRecurringId()).isEqualTo(recurring.getId());
	}

	@Test
	void shouldClampDayToEndOfMonth_whenMaterializing() {
		RecurringTransaction recurring = recurring(31, null, true);
		when(transactionRepository.findByRecurringIdAndDateBetween(any(), any(), any())).thenReturn(Optional.empty());
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

		Transaction created = service.materializeOccurrence(recurring, YearMonth.of(2026, 2));

		assertThat(created.getDate()).isEqualTo(LocalDate.of(2026, 2, 28));
	}

	@Test
	void shouldBeIdempotent_whenOccurrenceAlreadyExists() {
		RecurringTransaction recurring = recurring(10, null, true);
		Transaction existing = Transaction.materialized(userId, recurring.getAccountId(), recurring.getCategoryId(),
				"Spotify", new BigDecimal("27.90"), LocalDate.of(2026, 7, 10), TransactionType.EXPENSE,
				recurring.getId());
		when(transactionRepository.findByRecurringIdAndDateBetween(any(), any(), any()))
				.thenReturn(Optional.of(existing));

		Transaction result = service.materializeOccurrence(recurring, YearMonth.of(2026, 7));

		assertThat(result).isSameAs(existing);
		verify(transactionRepository, never()).save(any());
	}

	@Test
	void shouldMaterializeForUserAndReturnOccurrences() {
		RecurringTransaction recurring = recurring(10, null, true);
		when(recurringRepository.findAllByUserIdOrderByDescriptionAsc(userId)).thenReturn(List.of(recurring));
		when(transactionRepository.findByRecurringIdAndDateBetween(any(), any(), any())).thenReturn(Optional.empty());
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

		List<OccurrenceResponse> occurrences = service.materializeForUserAndMonth(userId, YearMonth.of(2026, 7));

		assertThat(occurrences).hasSize(1);
		assertThat(occurrences.getFirst().date()).isEqualTo(LocalDate.of(2026, 7, 10));
	}

	@Test
	void shouldSkipEndedRecurring_whenListingOccurrences() {
		RecurringTransaction ended = recurring(10, LocalDate.of(2026, 6, 30), true);
		when(recurringRepository.findAllByUserIdOrderByDescriptionAsc(userId)).thenReturn(List.of(ended));

		List<OccurrenceResponse> occurrences = service.occurrencesFor(userId, YearMonth.of(2026, 7));

		assertThat(occurrences).isEmpty();
	}

	@Test
	void shouldReportOccurrenceStatus_whenTransactionExists() {
		RecurringTransaction recurring = recurring(10, null, true);
		Transaction existing = Transaction.materialized(userId, recurring.getAccountId(), recurring.getCategoryId(),
				"Spotify", new BigDecimal("27.90"), LocalDate.of(2026, 7, 10), TransactionType.EXPENSE,
				recurring.getId());
		ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());
		existing.markPaid(true);
		when(recurringRepository.findAllByUserIdOrderByDescriptionAsc(userId)).thenReturn(List.of(recurring));
		when(transactionRepository.findByRecurringIdAndDateBetween(any(), any(), any()))
				.thenReturn(Optional.of(existing));

		List<OccurrenceResponse> occurrences = service.occurrencesFor(userId, YearMonth.of(2026, 7));

		assertThat(occurrences).hasSize(1);
		assertThat(occurrences.getFirst().materialized()).isTrue();
		assertThat(occurrences.getFirst().paid()).isTrue();
		assertThat(occurrences.getFirst().transactionId()).isEqualTo(existing.getId());
	}

	@Test
	void shouldMaterializeActiveRecurringsInScheduledJob() {
		RecurringTransaction active = recurring(10, null, true);
		when(recurringRepository.findAllByActiveTrue()).thenReturn(List.of(active));
		when(transactionRepository.findByRecurringIdAndDateBetween(any(), any(), any())).thenReturn(Optional.empty());
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

		service.materializeCurrentMonth();

		ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
		verify(transactionRepository).save(saved.capture());
		assertThat(saved.getValue().getRecurringId()).isEqualTo(active.getId());
	}

}
