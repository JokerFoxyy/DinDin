package com.dindin.api.invoice.dto;

import java.util.List;

public record InvoiceDetailResponse(InvoiceSummaryResponse invoice, List<InvoiceLine> transactions) {
}
