package com.dindin.api.dashboard;

import com.dindin.api.budget.BudgetService;
import com.dindin.api.budget.dto.BudgetReportResponse;
import com.dindin.api.category.Category;
import com.dindin.api.category.CategoryRepository;
import com.dindin.api.dashboard.dto.AnnualPointResponse;
import com.dindin.api.dashboard.dto.CategorySpendResponse;
import com.dindin.api.dashboard.dto.DashboardSummaryResponse;
import com.dindin.api.transaction.CategorySpent;
import com.dindin.api.transaction.TransactionRepository;
import com.dindin.api.transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class DashboardService {

	private static final LocalDate EPOCH_START = LocalDate.of(1970, 1, 1);

	private final TransactionRepository transactionRepository;
	private final CategoryRepository categoryRepository;
	private final BudgetService budgetService;

	public DashboardService(TransactionRepository transactionRepository, CategoryRepository categoryRepository,
			BudgetService budgetService) {
		this.transactionRepository = transactionRepository;
		this.categoryRepository = categoryRepository;
		this.budgetService = budgetService;
	}

	@Transactional(readOnly = true)
	public DashboardSummaryResponse summary(UUID userId, YearMonth month) {
		LocalDate start = month.atDay(1);
		LocalDate end = month.atEndOfMonth();

		BigDecimal income = sumByType(userId, TransactionType.INCOME, start, end);
		BigDecimal expense = sumByType(userId, TransactionType.EXPENSE, start, end);
		BigDecimal monthBalance = income.subtract(expense);

		BigDecimal cumulativeIncome = sumByType(userId, TransactionType.INCOME, EPOCH_START, end);
		BigDecimal cumulativeExpense = sumByType(userId, TransactionType.EXPENSE, EPOCH_START, end);
		BigDecimal cumulativeBalance = cumulativeIncome.subtract(cumulativeExpense);

		List<CategorySpent> spent = transactionRepository.sumExpensesByCategoryForMonth(userId, start, end);
		Map<UUID, Category> categories = categoryRepository
				.findAllById(spent.stream().map(CategorySpent::categoryId).toList()).stream()
				.collect(Collectors.toMap(Category::getId, category -> category));
		List<CategorySpendResponse> categorySpend = spent.stream()
				.map(entry -> CategorySpendResponse.from(categories.get(entry.categoryId()), entry.total()))
				.toList();

		List<BudgetReportResponse> budgetReport = budgetService.report(userId, month);

		return new DashboardSummaryResponse(income, expense, monthBalance, cumulativeBalance, categorySpend,
				budgetReport);
	}

	@Transactional(readOnly = true)
	public List<AnnualPointResponse> annual(UUID userId, YearMonth month) {
		return IntStream.rangeClosed(1, month.getMonthValue())
				.mapToObj(monthValue -> YearMonth.of(month.getYear(), monthValue))
				.map(point -> new AnnualPointResponse(point,
						sumByType(userId, TransactionType.INCOME, point.atDay(1), point.atEndOfMonth()),
						sumByType(userId, TransactionType.EXPENSE, point.atDay(1), point.atEndOfMonth())))
				.toList();
	}

	private BigDecimal sumByType(UUID userId, TransactionType type, LocalDate start, LocalDate end) {
		return transactionRepository.sumByTypeAndDateBetween(userId, type, start, end);
	}

}
