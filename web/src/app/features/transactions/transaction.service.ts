import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { PageResponse, Transaction, TransactionFilters, TransactionPayload } from './transaction.models';

const API = '/api/v1/transactions';

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private readonly http = inject(HttpClient);

  list(month: string, filters: TransactionFilters = {}, page = 0, size = 50): Observable<PageResponse<Transaction>> {
    let params = new HttpParams()
      .set('month', month)
      .set('page', page)
      .set('size', size);
    if (filters.accountId) {
      params = params.set('accountId', filters.accountId);
    }
    if (filters.categoryId) {
      params = params.set('categoryId', filters.categoryId);
    }
    if (filters.type) {
      params = params.set('type', filters.type);
    }
    if (filters.q) {
      params = params.set('q', filters.q);
    }
    if (filters.tag) {
      params = params.set('tag', filters.tag);
    }
    return this.http.get<PageResponse<Transaction>>(API, { params });
  }

  create(payload: TransactionPayload): Observable<Transaction> {
    return this.http.post<Transaction>(API, payload);
  }

  update(id: string, payload: TransactionPayload): Observable<Transaction> {
    return this.http.put<Transaction>(`${API}/${id}`, payload);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${API}/${id}`);
  }
}
