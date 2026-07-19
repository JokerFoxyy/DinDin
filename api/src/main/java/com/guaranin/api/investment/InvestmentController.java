package com.guaranin.api.investment;

import com.guaranin.api.common.security.AuthenticatedUser;
import com.guaranin.api.investment.dto.InvestmentEntryRequest;
import com.guaranin.api.investment.dto.InvestmentEntryResponse;
import com.guaranin.api.investment.dto.InvestmentReportResponse;
import com.guaranin.api.investment.dto.InvestmentRequest;
import com.guaranin.api.investment.dto.InvestmentResponse;
import com.guaranin.api.investment.dto.InvestmentUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/investments")
public class InvestmentController {

	private final InvestmentService investmentService;

	public InvestmentController(InvestmentService investmentService) {
		this.investmentService = investmentService;
	}

	@GetMapping
	public List<InvestmentResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
		return investmentService.list(user.id());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public InvestmentResponse create(@AuthenticationPrincipal AuthenticatedUser user,
			@Valid @RequestBody InvestmentRequest request) {
		return investmentService.create(user.id(), request);
	}

	@PutMapping("/{id}")
	public InvestmentResponse update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@Valid @RequestBody InvestmentUpdateRequest request) {
		return investmentService.update(user.id(), id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		investmentService.delete(user.id(), id);
	}

	@GetMapping("/{id}/entries")
	public List<InvestmentEntryResponse> listEntries(@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable UUID id) {
		return investmentService.listEntries(user.id(), id);
	}

	@PostMapping("/{id}/entries")
	@ResponseStatus(HttpStatus.CREATED)
	public InvestmentEntryResponse createEntry(@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable UUID id, @Valid @RequestBody InvestmentEntryRequest request) {
		return investmentService.createEntry(user.id(), id, request);
	}

	@DeleteMapping("/{id}/entries/{entryId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteEntry(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
			@PathVariable UUID entryId) {
		investmentService.deleteEntry(user.id(), id, entryId);
	}

	@GetMapping("/report")
	public InvestmentReportResponse report(@AuthenticationPrincipal AuthenticatedUser user) {
		return investmentService.report(user.id());
	}

}
