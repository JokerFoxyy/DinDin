package com.guaranin.api.transaction;

import com.guaranin.api.common.dto.PageResponse;
import com.guaranin.api.common.security.AuthenticatedUser;
import com.guaranin.api.transaction.dto.PaidRequest;
import com.guaranin.api.transaction.dto.TransactionRequest;
import com.guaranin.api.transaction.dto.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
	private final TransactionExportService transactionExportService;

	public TransactionController(TransactionService transactionService,
			TransactionExportService transactionExportService) {
		this.transactionService = transactionService;
		this.transactionExportService = transactionExportService;
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

	@GetMapping("/export")
	public ResponseEntity<byte[]> export(
			@AuthenticationPrincipal AuthenticatedUser user,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
			@RequestParam(required = false) UUID accountId,
			@RequestParam(required = false) UUID categoryId,
			@RequestParam(required = false) TransactionType type,
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String tag,
			@RequestParam(defaultValue = "csv") String format) {
		TransactionExportService.ExportFile file = transactionExportService.export(
				user.id(), month, accountId, categoryId, type, q, tag, format);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(file.contentType()))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(file.filename()).build().toString())
				.body(file.content());
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
	public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@RequestParam(required = false) String scope) {
		transactionService.delete(user.id(), id, scope);
	}

	@PutMapping("/{id}/paid")
	public TransactionResponse setPaid(@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable UUID id, @Valid @RequestBody PaidRequest request) {
		return transactionService.setPaid(user.id(), id, request.paid());
	}

}
