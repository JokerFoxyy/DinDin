import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { InvoiceDetail, InvoiceSummary } from './invoice.models';

const API = '/api/v1/invoices';

@Injectable({ providedIn: 'root' })
export class InvoiceService {
  private readonly http = inject(HttpClient);

  list(month: string): Observable<InvoiceSummary[]> {
    return this.http.get<InvoiceSummary[]>(API, { params: new HttpParams().set('month', month) });
  }

  detail(id: string): Observable<InvoiceDetail> {
    return this.http.get<InvoiceDetail>(`${API}/${id}`);
  }

  close(id: string, declaredTotal: number): Observable<InvoiceDetail> {
    return this.http.post<InvoiceDetail>(`${API}/${id}/close`, { declaredTotal });
  }

  pay(id: string): Observable<InvoiceDetail> {
    return this.http.post<InvoiceDetail>(`${API}/${id}/pay`, {});
  }

  reopen(id: string): Observable<InvoiceDetail> {
    return this.http.post<InvoiceDetail>(`${API}/${id}/reopen`, {});
  }
}
