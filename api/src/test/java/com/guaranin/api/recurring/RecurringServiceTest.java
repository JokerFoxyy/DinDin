package com.guaranin.api.recurring;

import com.guaranin.api.account.Account;
import com.guaranin.api.account.AccountRepository;
import com.guaranin.api.account.AccountType;
import com.guaranin.api.category.Category;
import com.guaranin.api.category.CategoryKind;
import com.guaranin.api.category.CategoryRepository;
import com.guaranin.api.common.error.BusinessException;
import com.guaranin.api.common.error.NotFoundException;
import com.guaranin.api.recurring.dto.RecurringRequest;
import com.guaranin.api.recurring.dto.RecurringResponse;
import com.guaranin.api.transaction.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringServiceTest {

	private final UUID userId = UUID.randomUUID();

	@Mock
	private RecurringTransactionRepository recurringRepository;
	@Mock
	private AccountRepository accountRepository;
	@Mock
	private CategoryRepository categoryRepository;

	@InjectMocks
	private RecurringService service;

	private Account account() {
		Account account = new Account(userId, "Uniclass", AccountType.CHECKING, null, null);
		ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
		return account;
	}

	private Category category(CategoryKind kind) {
		Category category = new Category(userId, "Assinaturas", "🔁", "#a371f7", kind);
		ReflectionTestUtils.setField(category, "id", UUID.randomUUID());
		return category;
	}

	private RecurringRequest request(Account account, Category category, TransactionType type) {
		return new RecurringRequest("Spotify", new BigDecimal("27.90"), type,
				account.getId(), category.getId(), 10, true, null);
	}

	@Test
	void shouldCreateRecurring_whenValid() {
		Account account = account();
		Category category = category(CategoryKind.EXPENSE);
		when(accountRepository.findByIdAndUserId(account.getId(), userId)).thenReturn(Optional.of(account));
		when(categoryRepository.findByIdAndUserId(category.getId(), userId)).thenReturn(Optional.of(category));
		when(recurringRepository.save(any(RecurringTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

		RecurringResponse response = service.create(userId, request(account, category, TransactionType.EXPENSE));

		assertThat(response.description()).isEqualTo("Spotify");
		assertThat(response.dayOfMonth()).isEqualTo(10);
		assertThat(response.active()).isTrue();
	}

	@Test
	void shouldThrowBusiness_whenTypeIsInvoiceAdjustment() {
		assertThatThrownBy(() -> service.create(userId, new RecurringRequest("x", BigDecimal.TEN,
				TransactionType.INVOICE_ADJUSTMENT, UUID.randomUUID(), UUID.randomUUID(), 10, true, null)))
				.isInstanceOf(BusinessException.class);
		verify(recurringRepository, never()).save(any());
	}

	@Test
	void shouldThrowBusiness_whenCategoryKindDoesNotMatchType() {
		Account account = account();
		Category income = category(CategoryKind.INCOME);
		when(accountRepository.findByIdAndUserId(account.getId(), userId)).thenReturn(Optional.of(account));
		when(categoryRepository.findByIdAndUserId(income.getId(), userId)).thenReturn(Optional.of(income));

		assertThatThrownBy(() -> service.create(userId, request(account, income, TransactionType.EXPENSE)))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	void shouldThrowNotFound_whenAccountBelongsToAnotherUser() {
		when(accountRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(userId, new RecurringRequest("x", BigDecimal.TEN,
				TransactionType.EXPENSE, UUID.randomUUID(), UUID.randomUUID(), 10, true, null)))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void shouldUpdateRecurring_whenOwned() {
		Account account = account();
		Category category = category(CategoryKind.EXPENSE);
		RecurringTransaction recurring = new RecurringTransaction(userId, account.getId(), category.getId(),
				"Spotify", new BigDecimal("27.90"), TransactionType.EXPENSE, 10, true, null);
		when(recurringRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.of(recurring));
		when(accountRepository.findByIdAndUserId(account.getId(), userId)).thenReturn(Optional.of(account));
		when(categoryRepository.findByIdAndUserId(category.getId(), userId)).thenReturn(Optional.of(category));

		RecurringResponse response = service.update(userId, UUID.randomUUID(),
				new RecurringRequest("Spotify Family", new BigDecimal("34.90"), TransactionType.EXPENSE,
						account.getId(), category.getId(), 15, false, null));

		assertThat(response.description()).isEqualTo("Spotify Family");
		assertThat(response.dayOfMonth()).isEqualTo(15);
		assertThat(response.active()).isFalse();
	}

	@Test
	void shouldThrowNotFound_whenUpdatingRecurringOfAnotherUser() {
		when(recurringRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(userId, UUID.randomUUID(), new RecurringRequest("x",
				BigDecimal.TEN, TransactionType.EXPENSE, UUID.randomUUID(), UUID.randomUUID(), 10, true, null)))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void shouldDeleteRecurring_whenOwned() {
		RecurringTransaction recurring = new RecurringTransaction(userId, UUID.randomUUID(), UUID.randomUUID(),
				"Spotify", new BigDecimal("27.90"), TransactionType.EXPENSE, 10, true, null);
		when(recurringRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.of(recurring));

		service.delete(userId, UUID.randomUUID());

		verify(recurringRepository).delete(recurring);
	}

	@Test
	void shouldListRecurringOfUser() {
		Account account = account();
		Category category = category(CategoryKind.EXPENSE);
		RecurringTransaction recurring = new RecurringTransaction(userId, account.getId(), category.getId(),
				"Academia", new BigDecimal("89.90"), TransactionType.EXPENSE, 5, true, null);
		when(recurringRepository.findAllByUserIdOrderByDescriptionAsc(userId)).thenReturn(List.of(recurring));
		when(accountRepository.findByIdAndUserId(account.getId(), userId)).thenReturn(Optional.of(account));
		when(categoryRepository.findByIdAndUserId(category.getId(), userId)).thenReturn(Optional.of(category));

		List<RecurringResponse> list = service.list(userId);

		assertThat(list).hasSize(1);
		assertThat(list.getFirst().description()).isEqualTo("Academia");
	}

}
