package com.dindin.api.account;

import com.dindin.api.auth.refresh.RefreshTokenRepository;
import com.dindin.api.category.Category;
import com.dindin.api.category.CategoryKind;
import com.dindin.api.category.CategoryRepository;
import com.dindin.api.common.error.NotFoundException;
import com.dindin.api.goal.Goal;
import com.dindin.api.goal.GoalRepository;
import com.dindin.api.invoice.CardInvoice;
import com.dindin.api.invoice.CardInvoiceRepository;
import com.dindin.api.investment.AssetClass;
import com.dindin.api.investment.Investment;
import com.dindin.api.investment.InvestmentRepository;
import com.dindin.api.transaction.Transaction;
import com.dindin.api.transaction.TransactionRepository;
import com.dindin.api.transaction.TransactionType;
import com.dindin.api.user.User;
import com.dindin.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDataServiceTest {

	private final UUID userId = UUID.randomUUID();

	@Mock
	private UserRepository userRepository;
	@Mock
	private AccountRepository accountRepository;
	@Mock
	private CategoryRepository categoryRepository;
	@Mock
	private TransactionRepository transactionRepository;
	@Mock
	private CardInvoiceRepository cardInvoiceRepository;
	@Mock
	private com.dindin.api.recurring.RecurringTransactionRepository recurringRepository;
	@Mock
	private RefreshTokenRepository refreshTokenRepository;
	@Mock
	private InvestmentRepository investmentRepository;
	@Mock
	private GoalRepository goalRepository;

	@InjectMocks
	private UserDataService service;

	private <T> T withId(T entity) {
		ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
		return entity;
	}

	private User user() {
		User user = new User("victor@dindin.com", "hash");
		ReflectionTestUtils.setField(user, "id", userId);
		ReflectionTestUtils.setField(user, "createdAt", Instant.now());
		return user;
	}

	@Test
	void shouldExportAllUserData() {
		Account account = withId(new Account(userId, "Uniclass", AccountType.CHECKING, null, null));
		Category category = withId(new Category(userId, "Mercado", "🛒", "#3fb950", CategoryKind.EXPENSE));
		Transaction transaction = withId(new Transaction(userId, account.getId(), category.getId(), null,
				"Padaria", new BigDecimal("31.73"), LocalDate.of(2026, 7, 9), TransactionType.EXPENSE));
		CardInvoice invoice = withId(new CardInvoice(account.getId(), LocalDate.of(2026, 7, 1),
				LocalDate.of(2026, 7, 28), LocalDate.of(2026, 8, 7)));
		Investment investment = withId(new Investment(userId, "Tesouro Selic", AssetClass.RENDA_FIXA, "NuInvest"));
		Goal goal = withId(new Goal(userId, "Reserva", new BigDecimal("12000.00"), LocalDate.of(2026, 12, 1)));

		when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user()));
		when(accountRepository.findAllByUserIdOrderByNameAsc(userId)).thenReturn(List.of(account));
		when(categoryRepository.findAllByUserIdOrderByNameAsc(userId)).thenReturn(List.of(category));
		when(transactionRepository.findAllByUserIdOrderByDateAsc(userId)).thenReturn(List.of(transaction));
		when(cardInvoiceRepository.findByAccountIdIn(anyList())).thenReturn(List.of(invoice));
		when(recurringRepository.findAllByUserIdOrderByDescriptionAsc(userId)).thenReturn(List.of());
		when(investmentRepository.findAllByUserIdOrderByCreatedAtAsc(userId)).thenReturn(List.of(investment));
		when(goalRepository.findAllByUserIdOrderByCreatedAtAsc(userId)).thenReturn(List.of(goal));

		Map<String, Object> export = service.export(userId);

		assertThat(export).containsKeys("exportedAt", "user", "accounts", "categories",
				"transactions", "cardInvoices", "recurringTransactions", "investments", "goals");
		assertThat((List<?>) export.get("transactions")).hasSize(1);
		assertThat(export.get("user").toString()).contains("victor@dindin.com");
	}

	@Test
	void shouldThrowNotFound_whenExportingUnknownUser() {
		when(userRepository.findById(userId)).thenReturn(java.util.Optional.empty());

		assertThatThrownBy(() -> service.export(userId)).isInstanceOf(NotFoundException.class);
	}

	@Test
	void shouldDeleteAllUserDataInFkSafeOrder() {
		Account account = withId(new Account(userId, "Uniclass", AccountType.CHECKING, null, null));
		when(userRepository.existsById(userId)).thenReturn(true);
		when(accountRepository.findAllByUserIdOrderByNameAsc(userId)).thenReturn(List.of(account));

		service.deleteAccount(userId);

		InOrder order = inOrder(transactionRepository, recurringRepository, cardInvoiceRepository,
				categoryRepository, accountRepository, investmentRepository, goalRepository, refreshTokenRepository,
				userRepository);
		order.verify(transactionRepository).deleteByUserId(userId);
		order.verify(recurringRepository).deleteByUserId(userId);
		order.verify(cardInvoiceRepository).deleteByAccountIdIn(List.of(account.getId()));
		order.verify(categoryRepository).deleteByUserId(userId);
		order.verify(accountRepository).deleteByUserId(userId);
		order.verify(investmentRepository).deleteByUserId(userId);
		order.verify(goalRepository).deleteByUserId(userId);
		order.verify(refreshTokenRepository).deleteByUserId(userId);
		order.verify(userRepository).deleteById(userId);
	}

	@Test
	void shouldNotCallInvoiceDelete_whenUserHasNoAccounts() {
		when(userRepository.existsById(userId)).thenReturn(true);
		when(accountRepository.findAllByUserIdOrderByNameAsc(userId)).thenReturn(List.of());

		service.deleteAccount(userId);

		verify(cardInvoiceRepository, never()).deleteByAccountIdIn(anyList());
		verify(userRepository).deleteById(userId);
	}

	@Test
	void shouldThrowNotFound_whenDeletingUnknownUser() {
		when(userRepository.existsById(userId)).thenReturn(false);

		assertThatThrownBy(() -> service.deleteAccount(userId)).isInstanceOf(NotFoundException.class);
	}

}
