import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  CdiPoint, Investment, InvestmentEntry, InvestmentEntryRequest, InvestmentReport,
  InvestmentRequest, InvestmentUpdateRequest
} from './investment.models';

const API = '/api/v1/investments';

@Injectable({ providedIn: 'root' })
export class InvestmentService {
  private readonly http = inject(HttpClient);

  list(): Observable<Investment[]> {
    return this.http.get<Investment[]>(API);
  }

  create(request: InvestmentRequest): Observable<Investment> {
    return this.http.post<Investment>(API, request);
  }

  update(id: string, request: InvestmentUpdateRequest): Observable<Investment> {
    return this.http.put<Investment>(`${API}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${API}/${id}`);
  }

  listEntries(investmentId: string): Observable<InvestmentEntry[]> {
    return this.http.get<InvestmentEntry[]>(`${API}/${investmentId}/entries`);
  }

  createEntry(investmentId: string, request: InvestmentEntryRequest): Observable<InvestmentEntry> {
    return this.http.post<InvestmentEntry>(`${API}/${investmentId}/entries`, request);
  }

  deleteEntry(investmentId: string, entryId: string): Observable<void> {
    return this.http.delete<void>(`${API}/${investmentId}/entries/${entryId}`);
  }

  report(): Observable<InvestmentReport> {
    return this.http.get<InvestmentReport>(`${API}/report`);
  }

  cdi(from: string, to: string): Observable<CdiPoint[]> {
    return this.http.get<CdiPoint[]>(`${API}/cdi`, { params: { from, to } });
  }
}
