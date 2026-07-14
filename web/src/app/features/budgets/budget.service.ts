import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { BudgetAmountPayload, BudgetCreatePayload, BudgetReport } from './budget.models';

const API = '/api/v1/budgets';

@Injectable({ providedIn: 'root' })
export class BudgetService {
  private readonly http = inject(HttpClient);

  report(month: string): Observable<BudgetReport[]> {
    return this.http.get<BudgetReport[]>(API, { params: { month } });
  }

  create(payload: BudgetCreatePayload): Observable<BudgetReport> {
    return this.http.post<BudgetReport>(API, payload);
  }

  updateAmount(id: string, payload: BudgetAmountPayload): Observable<BudgetReport> {
    return this.http.put<BudgetReport>(`${API}/${id}`, payload);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${API}/${id}`);
  }
}
