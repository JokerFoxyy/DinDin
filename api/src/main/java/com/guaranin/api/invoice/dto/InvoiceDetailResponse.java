package com.guaranin.api.invoice.dto;

import java.util.List;

public record InvoiceDetailResponse(InvoiceSummaryResponse invoice, List<InvoiceLine> transactions) {
}
