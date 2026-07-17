package com.dindin.api.transaction;

import com.dindin.api.common.dto.PageResponse;
import com.dindin.api.common.security.AuthenticatedUser;
import com.dindin.api.transaction.dto.PaidRequest;
import com.dindin.api.transaction.dto.TransactionRequest;
import com.dindin.api.transaction.dto.TransactionResponse;
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
import java.util.UUID;

@RestController
@RequestMapping("/v1/transactions")
public class TransactionController {

	private final TransactionService transactionService;

	public TransactionController(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

	@GetMapping
	public PageResponse<TransactionResponse> search(
			@AuthenticationPrincipal AuthenticatedUser user,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
			@RequestParam(required = false) UUID accountId,
			@RequestParam(required = false) UUID categoryId,
			@RequestParam(required = false) TransactionType type,
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String tag,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		return transactionService.search(user.id(), month, accountId, categoryId, type, q, tag, page, size);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TransactionResponse create(@AuthenticationPrincipal AuthenticatedUser user,
			@Valid @RequestBody TransactionRequest request) {
		return transactionService.create(user.id(), request);
	}

	@PutMapping("/{id}")
	public TransactionResponse update(@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable UUID id, @Valid @RequestBody TransactionRequest request) {
		return transactionService.update(user.id(), id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		transactionService.delete(user.id(), id);
	}

	@PutMapping("/{id}/paid")
	public TransactionResponse setPaid(@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable UUID id, @Valid @RequestBody PaidRequest request) {
		return transactionService.setPaid(user.id(), id, request.paid());
	}

}
