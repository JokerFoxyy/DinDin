import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Account } from './settings.models';

const API = '/api/v1/accounts';

export type AccountPayload = Omit<Account, 'id'>;

@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly http = inject(HttpClient);

  list(): Observable<Account[]> {
    return this.http.get<Account[]>(API);
  }

  create(payload: AccountPayload): Observable<Account> {
    return this.http.post<Account>(API, payload);
  }

  update(id: string, payload: AccountPayload): Observable<Account> {
    return this.http.put<Account>(`${API}/${id}`, payload);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${API}/${id}`);
  }
}
