export type InvoiceStatus = 'OPEN' | 'CLOSED' | 'PAID';

export interface InvoiceSummary {
  id: string;
  accountId: string;
  accountName: string | null;
  month: string;
  closingDate: string;
  dueDate: string;
  launchedTotal: number;
  declaredTotal: number | null;
  adjustment: number;
  status: InvoiceStatus;
}

export interface InvoiceLine {
  id: string;
  date: string;
  description: string;
  amount: number;
  type: string;
  categoryName: string | null;
  categoryIcon: string | null;
  categoryColor: string | null;
}

export interface InvoiceDetail {
  invoice: InvoiceSummary;
  transactions: InvoiceLine[];
}

export const INVOICE_STATUS_LABELS: Record<InvoiceStatus, string> = {
  OPEN: 'Aberta',
  CLOSED: 'Fechada',
  PAID: 'Paga'
};
