import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { MeResponse, TokenResponse } from './auth.models';

const TOKEN_KEY = 'dindin.token';
const API = '/api/v1/auth';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly tokenSignal = signal<string | null>(localStorage.getItem(TOKEN_KEY));

  readonly isAuthenticated = computed(() => this.tokenSignal() !== null);
  readonly currentUser = signal<MeResponse | null>(null);

  token(): string | null {
    return this.tokenSignal();
  }

  register(email: string, password: string): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>(`${API}/register`, { email, password })
      .pipe(tap((response) => this.storeToken(response.token)));
  }

  login(email: string, password: string): Observable<TokenResponse> {
    return this.http
      .post<TokenResponse>(`${API}/login`, { email, password })
      .pipe(tap((response) => this.storeToken(response.token)));
  }

  loadCurrentUser(): Observable<MeResponse> {
    return this.http
      .get<MeResponse>(`${API}/me`)
      .pipe(tap((user) => this.currentUser.set(user)));
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.tokenSignal.set(null);
    this.currentUser.set(null);
  }

  private storeToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
    this.tokenSignal.set(token);
  }
}
