import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AuthService } from './auth.service';
import { UserResponse } from './auth.models';

const AUTH_FLAG = 'guaranin.authed';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  const user: UserResponse = { id: 'u1', email: 'victor@guaranin.com' };

  beforeEach(() => {
    localStorage.removeItem(AUTH_FLAG);
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.removeItem(AUTH_FLAG);
  });

  it('should not be authenticated initially', () => {
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('should authenticate and store the flag on login', () => {
    service.login('victor@guaranin.com', 'senha-forte-123').subscribe();

    const request = httpMock.expectOne('/api/v1/auth/login');
    expect(request.request.method).toBe('POST');
    request.flush(user);

    expect(service.isAuthenticated()).toBeTrue();
    expect(service.currentUser()).toEqual(user);
    expect(localStorage.getItem(AUTH_FLAG)).toBe('1');
  });

  it('should authenticate on register', () => {
    service.register('novo@guaranin.com', 'senha-forte-123').subscribe();
    httpMock.expectOne('/api/v1/auth/register').flush(user);

    expect(service.isAuthenticated()).toBeTrue();
  });

  it('should re-authenticate on refresh', () => {
    service.refresh().subscribe();
    const request = httpMock.expectOne('/api/v1/auth/refresh');
    expect(request.request.method).toBe('POST');
    request.flush(user);

    expect(service.isAuthenticated()).toBeTrue();
  });

  it('should set the current user on loadCurrentUser', () => {
    service.loadCurrentUser().subscribe();
    httpMock.expectOne('/api/v1/auth/me').flush(user);

    expect(service.currentUser()?.email).toBe('victor@guaranin.com');
  });

  it('should call logout endpoint and clear the session', () => {
    service.login('victor@guaranin.com', 'senha-forte-123').subscribe();
    httpMock.expectOne('/api/v1/auth/login').flush(user);

    service.logout().subscribe();
    const request = httpMock.expectOne('/api/v1/auth/logout');
    expect(request.request.method).toBe('POST');
    request.flush(null);

    expect(service.isAuthenticated()).toBeFalse();
    expect(service.currentUser()).toBeNull();
    expect(localStorage.getItem(AUTH_FLAG)).toBeNull();
  });

  it('should clear session locally without a network call', () => {
    service.login('victor@guaranin.com', 'senha-forte-123').subscribe();
    httpMock.expectOne('/api/v1/auth/login').flush(user);

    service.clearSession();

    expect(service.isAuthenticated()).toBeFalse();
    expect(localStorage.getItem(AUTH_FLAG)).toBeNull();
  });
});
