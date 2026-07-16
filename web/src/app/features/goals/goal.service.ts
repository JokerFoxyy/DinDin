import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Goal, GoalContribution, GoalContributionRequest, GoalRequest } from './goal.models';

const API = '/api/v1/goals';

@Injectable({ providedIn: 'root' })
export class GoalService {
  private readonly http = inject(HttpClient);

  list(): Observable<Goal[]> {
    return this.http.get<Goal[]>(API);
  }

  create(request: GoalRequest): Observable<Goal> {
    return this.http.post<Goal>(API, request);
  }

  update(id: string, request: GoalRequest): Observable<Goal> {
    return this.http.put<Goal>(`${API}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${API}/${id}`);
  }

  listContributions(goalId: string): Observable<GoalContribution[]> {
    return this.http.get<GoalContribution[]>(`${API}/${goalId}/contributions`);
  }

  createContribution(goalId: string, request: GoalContributionRequest): Observable<GoalContribution> {
    return this.http.post<GoalContribution>(`${API}/${goalId}/contributions`, request);
  }

  deleteContribution(goalId: string, contributionId: string): Observable<void> {
    return this.http.delete<void>(`${API}/${goalId}/contributions/${contributionId}`);
  }
}
