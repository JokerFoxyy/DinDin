import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AuthService } from './auth.service';
import { TokenResponse } from './auth.models';

const TOKEN_KEY = 'dindin.token';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  const tokenResponse: TokenResponse = { token: 'jwt-abc', tokenType: 'Bearer', expiresInSeconds: 7200 };

  beforeEach(() => {
    localStorage.removeItem(TOKEN_KEY);
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.removeItem(TOKEN_KEY);
  });

  it('should not be authenticated when there is no stored token', () => {
    expect(service.isAuthenticated()).toBeFalse();
    expect(service.token()).toBeNull();
  });

  it('should store token and authenticate when login succeeds', () => {
    service.login('victor@dindin.com', 'senha-forte-123').subscribe();

    const request = httpMock.expectOne('/api/v1/auth/login');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ email: 'victor@dindin.com', password: 'senha-forte-123' });
    request.flush(tokenResponse);

    expect(service.token()).toBe('jwt-abc');
    expect(service.isAuthenticated()).toBeTrue();
    expect(localStorage.getItem(TOKEN_KEY)).toBe('jwt-abc');
  });

  it('should store token when register succeeds', () => {
    service.register('novo@dindin.com', 'senha-forte-123').subscribe();

    const request = httpMock.expectOne('/api/v1/auth/register');
    expect(request.request.method).toBe('POST');
    request.flush(tokenResponse);

    expect(service.isAuthenticated()).toBeTrue();
  });

  it('should set currentUser when loadCurrentUser succeeds', () => {
    service.loadCurrentUser().subscribe();

    const request = httpMock.expectOne('/api/v1/auth/me');
    expect(request.request.method).toBe('GET');
    request.flush({ id: 'uuid-1', email: 'victor@dindin.com' });

    expect(service.currentUser()?.email).toBe('victor@dindin.com');
  });

  it('should clear token and user when logout is called', () => {
    service.login('victor@dindin.com', 'senha-forte-123').subscribe();
    httpMock.expectOne('/api/v1/auth/login').flush(tokenResponse);

    service.logout();

    expect(service.token()).toBeNull();
    expect(service.isAuthenticated()).toBeFalse();
    expect(service.currentUser()).toBeNull();
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
  });
});
