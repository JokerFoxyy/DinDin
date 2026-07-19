package com.guaranin.api.budget;

import com.guaranin.api.budget.dto.BudgetAmountRequest;
import com.guaranin.api.budget.dto.BudgetReportResponse;
import com.guaranin.api.budget.dto.BudgetRequest;
import com.guaranin.api.category.Category;
import com.guaranin.api.category.CategoryKind;
import com.guaranin.api.category.CategoryRepository;
import com.guaranin.api.common.error.BusinessException;
import com.guaranin.api.common.error.DuplicateResourceException;
import com.guaranin.api.common.error.NotFoundException;
import com.guaranin.api.transaction.CategorySpent;
import com.guaranin.api.transaction.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BudgetService {

	private final BudgetRepository budgetRepository;
	private final CategoryRepository categoryRepository;
	private final TransactionRepository transactionRepository;

	public BudgetService(BudgetRepository budgetRepository, CategoryRepository categoryRepository,
			TransactionRepository transactionRepository) {
		this.budgetRepository = budgetRepository;
		this.categoryRepository = categoryRepository;
		this.transactionRepository = transactionRepository;
	}

	@Transactional(readOnly = true)
	public List<BudgetReportResponse> report(UUID userId, YearMonth month) {
		List<Budget> budgets = budgetRepository.findAllByUserIdAndMonthOrderByCreatedAtAsc(userId, month.atDay(1));
		if (budgets.isEmpty()) {
			return List.of();
		}
		List<UUID> categoryIds = budgets.stream().map(Budget::getCategoryId).toList();
		Map<UUID, Category> categories = categoryRepository.findAllById(categoryIds).stream()
				.collect(Collectors.toMap(Category::getId, category -> category));
		Map<UUID, BigDecimal> spentByCategory = transactionRepository
				.sumExpensesByCategory(userId, categoryIds, month.atDay(1), month.atEndOfMonth()).stream()
				.collect(Collectors.toMap(CategorySpent::categoryId, CategorySpent::total));
		return budgets.stream()
				.map(budget -> BudgetReportResponse.from(budget, categories.get(budget.getCategoryId()),
						spentByCategory.getOrDefault(budget.getCategoryId(), BigDecimal.ZERO)))
				.toList();
	}

	/** Subconjunto do relatório com apenas as categorias que estouraram o orçamento no mês. */
	@Transactional(readOnly = true)
	public List<BudgetReportResponse> alerts(UUID userId, YearMonth month) {
		return report(userId, month).stream().filter(BudgetReportResponse::over).toList();
	}

	@Transactional
	public BudgetReportResponse create(UUID userId, BudgetRequest request) {
		Category category = validateCategory(userId, request.categoryId());
		LocalDate month = request.month().atDay(1);
		if (budgetRepository.existsByUserIdAndCategoryIdAndMonth(userId, category.getId(), month)) {
			throw new DuplicateResourceException("Já existe orçamento para essa categoria neste mês");
		}
		Budget budget = budgetRepository.save(new Budget(userId, category.getId(), month, request.amount()));
		BigDecimal spent = spentFor(userId, category.getId(), request.month());
		return BudgetReportResponse.from(budget, category, spent);
	}

	@Transactional
	public BudgetReportResponse updateAmount(UUID userId, UUID budgetId, BudgetAmountRequest request) {
		Budget budget = findOwned(userId, budgetId);
		budget.updateAmount(request.amount());
		Category category = categoryRepository.findById(budget.getCategoryId())
				.orElseThrow(() -> new NotFoundException("Categoria não encontrada"));
		BigDecimal spent = spentFor(userId, budget.getCategoryId(), YearMonth.from(budget.getMonth()));
		return BudgetReportResponse.from(budget, category, spent);
	}

	@Transactional
	public void delete(UUID userId, UUID budgetId) {
		budgetRepository.delete(findOwned(userId, budgetId));
	}

	private BigDecimal spentFor(UUID userId, UUID categoryId, YearMonth month) {
		return transactionRepository
				.sumExpensesByCategory(userId, List.of(categoryId), month.atDay(1), month.atEndOfMonth()).stream()
				.map(CategorySpent::total)
				.findFirst()
				.orElse(BigDecimal.ZERO);
	}

	private Category validateCategory(UUID userId, UUID categoryId) {
		Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
				.orElseThrow(() -> new NotFoundException("Categoria não encontrada"));
		if (category.getKind() != CategoryKind.EXPENSE) {
			throw new BusinessException("Orçamento só pode ser definido para categoria de gasto");
		}
		return category;
	}

	private Budget findOwned(UUID userId, UUID budgetId) {
		return budgetRepository.findByIdAndUserId(budgetId, userId)
				.orElseThrow(() -> new NotFoundException("Orçamento não encontrado"));
	}

}
