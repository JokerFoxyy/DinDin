package com.dindin.api.dashboard;

import com.dindin.api.budget.BudgetService;
import com.dindin.api.budget.dto.BudgetReportResponse;
import com.dindin.api.category.Category;
import com.dindin.api.category.CategoryKind;
import com.dindin.api.category.CategoryRepository;
import com.dindin.api.dashboard.dto.AnnualPointResponse;
import com.dindin.api.dashboard.dto.DashboardSummaryResponse;
import com.dindin.api.transaction.CategorySpent;
import com.dindin.api.transaction.TransactionRepository;
import com.dindin.api.transaction.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

	private final UUID userId = UUID.randomUUID();
	private final UUID categoryId = UUID.randomUUID();
	private final YearMonth month = YearMonth.of(2026, 7);

	@Mock
	private TransactionRepository transactionRepository;
	@Mock
	private CategoryRepository categoryRepository;
	@Mock
	private BudgetService budgetService;

	@InjectMocks
	private DashboardService service;

	private Category category;

	@BeforeEach
	void setUp() {
		category = new Category(userId, "Mercado", "🛒", "#3fb950", CategoryKind.EXPENSE);
		ReflectionTestUtils.setField(category, "id", categoryId);
	}

	@Test
	void shouldComputeMonthAndCumulativeBalance() {
		when(transactionRepository.sumByTypeAndDateBetween(eq(userId), eq(TransactionType.INCOME),
				eq(month.atDay(1)), eq(month.atEndOfMonth()))).thenReturn(new BigDecimal("5000.00"));
		when(transactionRepository.sumByTypeAndDateBetween(eq(userId), eq(TransactionType.EXPENSE),
				eq(month.atDay(1)), eq(month.atEndOfMonth()))).thenReturn(new BigDecimal("3000.00"));
		when(transactionRepository.sumByTypeAndDateBetween(eq(userId), eq(TransactionType.INCOME),
				eq(LocalDate.of(1970, 1, 1)), eq(month.atEndOfMonth()))).thenReturn(new BigDecimal("20000.00"));
		when(transactionRepository.sumByTypeAndDateBetween(eq(userId), eq(TransactionType.EXPENSE),
				eq(LocalDate.of(1970, 1, 1)), eq(month.atEndOfMonth()))).thenReturn(new BigDecimal("15500.00"));
		when(transactionRepository.sumExpensesByCategoryForMonth(userId, month.atDay(1), month.atEndOfMonth()))
				.thenReturn(List.of());
		when(budgetService.report(userId, month)).thenReturn(List.of());

		DashboardSummaryResponse response = service.summary(userId, month);

		assertThat(response.income()).isEqualByComparingTo("5000.00");
		assertThat(response.expense()).isEqualByComparingTo("3000.00");
		assertThat(response.monthBalance()).isEqualByComparingTo("2000.00");
		assertThat(response.cumulativeBalance()).isEqualByComparingTo("4500.00");
	}

	@Test
	void shouldIncludeCategorySpendWithCategoryDetails() {
		lenient().when(transactionRepository.sumByTypeAndDateBetween(any(), any(), any(), any()))
				.thenReturn(BigDecimal.ZERO);
		when(transactionRepository.sumExpensesByCategoryForMonth(userId, month.atDay(1), month.atEndOfMonth()))
				.thenReturn(List.of(new CategorySpent(categoryId, new BigDecimal("300.00"))));
		when(categoryRepository.findAllById(List.of(categoryId))).thenReturn(List.of(category));
		when(budgetService.report(userId, month)).thenReturn(List.of());

		DashboardSummaryResponse response = service.summary(userId, month);

		assertThat(response.categorySpend()).hasSize(1);
		assertThat(response.categorySpend().getFirst().categoryName()).isEqualTo("Mercado");
		assertThat(response.categorySpend().getFirst().total()).isEqualByComparingTo("300.00");
	}

	@Test
	void shouldIncludeBudgetReportFromBudgetService() {
		lenient().when(transactionRepository.sumByTypeAndDateBetween(any(), any(), any(), any()))
				.thenReturn(BigDecimal.ZERO);
		when(transactionRepository.sumExpensesByCategoryForMonth(userId, month.atDay(1), month.atEndOfMonth()))
				.thenReturn(List.of());
		BudgetReportResponse budgetReport = new BudgetReportResponse(UUID.randomUUID(), categoryId, "Mercado",
				"🛒", "#3fb950", new BigDecimal("500.00"), new BigDecimal("300.00"), new BigDecimal("60"), false);
		when(budgetService.report(userId, month)).thenReturn(List.of(budgetReport));

		DashboardSummaryResponse response = service.summary(userId, month);

		assertThat(response.budgetReport()).containsExactly(budgetReport);
	}

	@Test
	void shouldReturnAnnualSeriesFromJanuaryToSelectedMonth() {
		lenient().when(transactionRepository.sumByTypeAndDateBetween(any(), any(), any(), any()))
				.thenReturn(BigDecimal.ZERO);
		when(transactionRepository.sumByTypeAndDateBetween(userId, TransactionType.INCOME,
				YearMonth.of(2026, 3).atDay(1), YearMonth.of(2026, 3).atEndOfMonth()))
				.thenReturn(new BigDecimal("1000.00"));

		List<AnnualPointResponse> series = service.annual(userId, YearMonth.of(2026, 3));

		assertThat(series).hasSize(3);
		assertThat(series.get(0).month()).isEqualTo(YearMonth.of(2026, 1));
		assertThat(series.get(2).month()).isEqualTo(YearMonth.of(2026, 3));
		assertThat(series.get(2).income()).isEqualByComparingTo("1000.00");
	}

}
