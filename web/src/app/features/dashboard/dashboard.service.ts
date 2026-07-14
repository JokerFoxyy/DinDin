import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AnnualPoint, DashboardSummary } from './dashboard.models';

const API = '/api/v1/dashboard';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  summary(month: string): Observable<DashboardSummary> {
    return this.http.get<DashboardSummary>(`${API}/summary`, { params: { month } });
  }

  annual(month: string): Observable<AnnualPoint[]> {
    return this.http.get<AnnualPoint[]>(`${API}/annual`, { params: { month } });
  }
}
