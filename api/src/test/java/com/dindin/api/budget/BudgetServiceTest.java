package com.dindin.api.budget;

import com.dindin.api.budget.dto.BudgetAmountRequest;
import com.dindin.api.budget.dto.BudgetReportResponse;
import com.dindin.api.budget.dto.BudgetRequest;
import com.dindin.api.category.Category;
import com.dindin.api.category.CategoryKind;
import com.dindin.api.category.CategoryRepository;
import com.dindin.api.common.error.BusinessException;
import com.dindin.api.common.error.DuplicateResourceException;
import com.dindin.api.common.error.NotFoundException;
import com.dindin.api.transaction.CategorySpent;
import com.dindin.api.transaction.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

	private final UUID userId = UUID.randomUUID();
	private final UUID categoryId = UUID.randomUUID();
	private final UUID budgetId = UUID.randomUUID();
	private final YearMonth month = YearMonth.of(2026, 7);

	@Mock
	private BudgetRepository budgetRepository;
	@Mock
	private CategoryRepository categoryRepository;
	@Mock
	private TransactionRepository transactionRepository;

	@InjectMocks
	private BudgetService service;

	private Category expenseCategory;
	private Budget budget;

	@BeforeEach
	void setUp() {
		expenseCategory = new Category(userId, "Mercado", "🛒", "#ff0000", CategoryKind.EXPENSE);
		ReflectionTestUtils.setField(expenseCategory, "id", categoryId);
		budget = new Budget(userId, categoryId, month.atDay(1), new BigDecimal("500.00"));
		ReflectionTestUtils.setField(budget, "id", budgetId);
	}

	@Test
	void shouldReturnEmptyReport_whenNoBudgetsForMonth() {
		when(budgetRepository.findAllByUserIdAndMonthOrderByCreatedAtAsc(userId, month.atDay(1)))
				.thenReturn(List.of());

		assertThat(service.report(userId, month)).isEmpty();
	}

	@Test
	void shouldComputePercentageAndOver_whenSpentExceedsBudgeted() {
		when(budgetRepository.findAllByUserIdAndMonthOrderByCreatedAtAsc(userId, month.atDay(1)))
				.thenReturn(List.of(budget));
		when(categoryRepository.findAllById(List.of(categoryId))).thenReturn(List.of(expenseCategory));
		when(transactionRepository.sumExpensesByCategory(userId, List.of(categoryId), month.atDay(1), month.atEndOfMonth()))
				.thenReturn(List.of(new CategorySpent(categoryId, new BigDecimal("600.00"))));

		List<BudgetReportResponse> report = service.report(userId, month);

		assertThat(report).hasSize(1);
		BudgetReportResponse item = report.getFirst();
		assertThat(item.spent()).isEqualByComparingTo("600.00");
		assertThat(item.percentage()).isEqualByComparingTo("120");
		assertThat(item.over()).isTrue();
	}

	@Test
	void shouldReportZeroSpent_whenNoTransactionsInMonth() {
		when(budgetRepository.findAllByUserIdAndMonthOrderByCreatedAtAsc(userId, month.atDay(1)))
				.thenReturn(List.of(budget));
		when(categoryRepository.findAllById(List.of(categoryId))).thenReturn(List.of(expenseCategory));
		when(transactionRepository.sumExpensesByCategory(userId, List.of(categoryId), month.atDay(1), month.atEndOfMonth()))
				.thenReturn(List.of());

		List<BudgetReportResponse> report = service.report(userId, month);

		assertThat(report.getFirst().spent()).isEqualByComparingTo("0");
		assertThat(report.getFirst().over()).isFalse();
	}

	@Test
	void shouldReturnOnlyOverBudgetCategories_inAlerts() {
		Budget okBudget = new Budget(userId, UUID.randomUUID(), month.atDay(1), new BigDecimal("500.00"));
		ReflectionTestUtils.setField(okBudget, "id", UUID.randomUUID());
		Category okCategory = new Category(userId, "Lazer", "🎮", "#a371f7", CategoryKind.EXPENSE);
		ReflectionTestUtils.setField(okCategory, "id", okBudget.getCategoryId());

		when(budgetRepository.findAllByUserIdAndMonthOrderByCreatedAtAsc(userId, month.atDay(1)))
				.thenReturn(List.of(budget, okBudget));
		when(categoryRepository.findAllById(List.of(categoryId, okBudget.getCategoryId())))
				.thenReturn(List.of(expenseCategory, okCategory));
		when(transactionRepository.sumExpensesByCategory(userId, List.of(categoryId, okBudget.getCategoryId()),
				month.atDay(1), month.atEndOfMonth())).thenReturn(List.of(
						new CategorySpent(categoryId, new BigDecimal("600.00")),
						new CategorySpent(okBudget.getCategoryId(), new BigDecimal("100.00"))));

		List<BudgetReportResponse> alerts = service.alerts(userId, month);

		assertThat(alerts).hasSize(1);
		assertThat(alerts.getFirst().categoryId()).isEqualTo(categoryId);
	}

	@Test
	void shouldCreateBudget_whenCategoryIsExpenseAndNotDuplicated() {
		when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(expenseCategory));
		when(budgetRepository.existsByUserIdAndCategoryIdAndMonth(userId, categoryId, month.atDay(1))).thenReturn(false);
		when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
			Budget saved = invocation.getArgument(0);
			ReflectionTestUtils.setField(saved, "id", budgetId);
			return saved;
		});
		when(transactionRepository.sumExpensesByCategory(any(), anyCollection(), any(), any())).thenReturn(List.of());

		BudgetReportResponse response = service.create(userId, new BudgetRequest(categoryId, month, new BigDecimal("500.00")));

		assertThat(response.id()).isEqualTo(budgetId);
		assertThat(response.budgeted()).isEqualByComparingTo("500.00");
	}

	@Test
	void shouldThrowDuplicate_whenBudgetAlreadyExistsForCategoryAndMonth() {
		when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(expenseCategory));
		when(budgetRepository.existsByUserIdAndCategoryIdAndMonth(userId, categoryId, month.atDay(1))).thenReturn(true);

		assertThatThrownBy(() -> service.create(userId, new BudgetRequest(categoryId, month, new BigDecimal("500.00"))))
				.isInstanceOf(DuplicateResourceException.class);
		verify(budgetRepository, never()).save(any());
	}

	@Test
	void shouldThrowBusiness_whenCategoryIsIncome() {
		Category income = new Category(userId, "Salário", null, null, CategoryKind.INCOME);
		ReflectionTestUtils.setField(income, "id", categoryId);
		when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(income));

		assertThatThrownBy(() -> service.create(userId, new BudgetRequest(categoryId, month, new BigDecimal("500.00"))))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	void shouldThrowNotFound_whenCategoryBelongsToAnotherUser() {
		when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(userId, new BudgetRequest(categoryId, month, new BigDecimal("500.00"))))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void shouldUpdateAmount_whenBudgetOwnedByUser() {
		lenient().when(budgetRepository.findByIdAndUserId(budgetId, userId)).thenReturn(Optional.of(budget));
		when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(expenseCategory));
		when(transactionRepository.sumExpensesByCategory(any(), anyCollection(), any(), any())).thenReturn(List.of());

		BudgetReportResponse response = service.updateAmount(userId, budgetId, new BudgetAmountRequest(new BigDecimal("700.00")));

		assertThat(response.budgeted()).isEqualByComparingTo("700.00");
		assertThat(budget.getAmount()).isEqualByComparingTo("700.00");
	}

	@Test
	void shouldThrowNotFound_whenUpdatingBudgetNotOwned() {
		when(budgetRepository.findByIdAndUserId(budgetId, userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.updateAmount(userId, budgetId, new BudgetAmountRequest(new BigDecimal("700.00"))))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void shouldDeleteBudget_whenOwnedByUser() {
		when(budgetRepository.findByIdAndUserId(budgetId, userId)).thenReturn(Optional.of(budget));

		service.delete(userId, budgetId);

		ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
		verify(budgetRepository).delete(captor.capture());
		assertThat(captor.getValue().getId()).isEqualTo(budgetId);
	}

	@Test
	void shouldThrowNotFound_whenDeletingBudgetNotOwned() {
		when(budgetRepository.findByIdAndUserId(budgetId, userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(userId, budgetId)).isInstanceOf(NotFoundException.class);
	}

}
