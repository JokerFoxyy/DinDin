package com.poupito.api.budget;

import com.poupito.api.budget.dto.BudgetAmountRequest;
import com.poupito.api.budget.dto.BudgetReportResponse;
import com.poupito.api.budget.dto.BudgetRequest;
import com.poupito.api.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/budgets")
public class BudgetController {

	private final BudgetService budgetService;

	public BudgetController(BudgetService budgetService) {
		this.budgetService = budgetService;
	}

	@GetMapping
	public List<BudgetReportResponse> report(@AuthenticationPrincipal AuthenticatedUser user,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
		return budgetService.report(user.id(), month);
	}

	@GetMapping("/alerts")
	public List<BudgetReportResponse> alerts(@AuthenticationPrincipal AuthenticatedUser user,
			@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
		return budgetService.alerts(user.id(), month != null ? month : YearMonth.now());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BudgetReportResponse create(@AuthenticationPrincipal AuthenticatedUser user,
			@Valid @RequestBody BudgetRequest request) {
		return budgetService.create(user.id(), request);
	}

	@PutMapping("/{id}")
	public BudgetReportResponse updateAmount(@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable UUID id, @Valid @RequestBody BudgetAmountRequest request) {
		return budgetService.updateAmount(user.id(), id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		budgetService.delete(user.id(), id);
	}

}
