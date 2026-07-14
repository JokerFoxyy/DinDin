import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';

import { MonthPicker } from '../../shared/month-picker';
import { InvoiceService } from './invoice.service';
import { INVOICE_STATUS_LABELS, InvoiceDetail, InvoiceStatus, InvoiceSummary } from './invoice.models';

@Component({
  selector: 'app-invoices',
  imports: [MonthPicker, CurrencyPipe, DatePipe],
  templateUrl: './invoices.html',
  styleUrl: './invoices.css'
})
export class Invoices implements OnInit {
  private readonly invoiceService = inject(InvoiceService);

  readonly month = signal(currentMonth());
  readonly invoices = signal<InvoiceSummary[]>([]);
  readonly errorMessage = signal<string | null>(null);

  readonly closingId = signal<string | null>(null);
  readonly declaredInput = signal('');
  readonly details = signal<Record<string, InvoiceDetail>>({});

  readonly statusLabels = INVOICE_STATUS_LABELS;

  ngOnInit(): void {
    this.load();
  }

  onMonthChange(month: string): void {
    this.month.set(month);
    this.details.set({});
    this.load();
  }

  statusClass(status: InvoiceStatus): string {
    return 'status-' + status.toLowerCase();
  }

  startClose(invoice: InvoiceSummary): void {
    this.closingId.set(invoice.id);
    this.declaredInput.set(invoice.launchedTotal.toFixed(2));
    this.errorMessage.set(null);
  }

  cancelClose(): void {
    this.closingId.set(null);
  }

  onDeclaredInput(value: string): void {
    this.declaredInput.set(value);
  }

  confirmClose(invoice: InvoiceSummary): void {
    const declared = Number(this.declaredInput());
    if (!Number.isFinite(declared) || declared < 0) {
      this.errorMessage.set('Informe o valor da fatura');
      return;
    }
    this.invoiceService.close(invoice.id, declared).subscribe({
      next: () => {
        this.closingId.set(null);
        this.reloadKeepingDetail(invoice.id);
      },
      error: () => this.errorMessage.set('Erro ao fechar a fatura')
    });
  }

  pay(invoice: InvoiceSummary): void {
    this.invoiceService.pay(invoice.id).subscribe({
      next: () => this.reloadKeepingDetail(invoice.id),
      error: () => this.errorMessage.set('Erro ao pagar a fatura')
    });
  }

  reopen(invoice: InvoiceSummary): void {
    this.invoiceService.reopen(invoice.id).subscribe({
      next: () => this.reloadKeepingDetail(invoice.id),
      error: () => this.errorMessage.set('Erro ao reabrir a fatura')
    });
  }

  toggleDetail(invoice: InvoiceSummary): void {
    const current = this.details();
    if (current[invoice.id]) {
      const { [invoice.id]: _removed, ...rest } = current;
      this.details.set(rest);
      return;
    }
    this.loadDetail(invoice.id);
  }

  isExpanded(invoiceId: string): boolean {
    return !!this.details()[invoiceId];
  }

  private load(): void {
    this.invoiceService.list(this.month()).subscribe((invoices) => this.invoices.set(invoices));
  }

  private reloadKeepingDetail(invoiceId: string): void {
    this.load();
    if (this.isExpanded(invoiceId)) {
      this.loadDetail(invoiceId);
    }
  }

  private loadDetail(invoiceId: string): void {
    this.invoiceService.detail(invoiceId).subscribe((detail) => {
      this.details.set({ ...this.details(), [invoiceId]: detail });
    });
  }
}

function currentMonth(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}
