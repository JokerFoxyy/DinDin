package com.guaranin.api.invoice;

import com.guaranin.api.common.security.AuthenticatedUser;
import com.guaranin.api.invoice.dto.CloseInvoiceRequest;
import com.guaranin.api.invoice.dto.InvoiceDetailResponse;
import com.guaranin.api.invoice.dto.InvoiceSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/invoices")
public class InvoiceController {

	private final InvoiceService invoiceService;

	public InvoiceController(InvoiceService invoiceService) {
		this.invoiceService = invoiceService;
	}

	@GetMapping
	public List<InvoiceSummaryResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
			@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
		return invoiceService.list(user.id(), month != null ? month : YearMonth.now());
	}

	@GetMapping("/{id}")
	public InvoiceDetailResponse detail(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		return invoiceService.getDetail(user.id(), id);
	}

	@PostMapping("/{id}/close")
	public InvoiceDetailResponse close(@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable UUID id, @Valid @RequestBody CloseInvoiceRequest request) {
		return invoiceService.close(user.id(), id, request.declaredTotal());
	}

	@PostMapping("/{id}/pay")
	public InvoiceDetailResponse pay(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		return invoiceService.pay(user.id(), id);
	}

	@PostMapping("/{id}/reopen")
	public InvoiceDetailResponse reopen(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
		return invoiceService.reopen(user.id(), id);
	}

}
