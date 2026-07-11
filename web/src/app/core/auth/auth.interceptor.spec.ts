import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authService: AuthService;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: router }
      ]
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
  });

  afterEach(() => httpMock.verify());

  it('should send requests with credentials so cookies flow', () => {
    http.get('/api/v1/accounts').subscribe();

    const request = httpMock.expectOne('/api/v1/accounts');
    expect(request.request.withCredentials).toBeTrue();
    request.flush([]);
  });

  it('should refresh once and retry when a 401 occurs on an app call', () => {
    spyOn(authService, 'isAuthenticated').and.returnValue(true);
    spyOn(authService, 'refresh').and.returnValue(of({ id: 'u1', email: 'e' }));
    let result: unknown;
    http.get('/api/v1/accounts').subscribe((r) => (result = r));

    httpMock.expectOne('/api/v1/accounts').flush(null, { status: 401, statusText: 'Unauthorized' });
    httpMock.expectOne('/api/v1/accounts').flush([{ id: 'a1' }]);

    expect(authService.refresh).toHaveBeenCalledTimes(1);
    expect(result).toEqual([{ id: 'a1' }]);
  });

  it('should redirect to login when refresh fails', () => {
    spyOn(authService, 'isAuthenticated').and.returnValue(true);
    spyOn(authService, 'refresh').and.returnValue(throwError(() => new Error('401')));
    const clearSpy = spyOn(authService, 'clearSession');
    http.get('/api/v1/accounts').subscribe({ error: () => {} });

    httpMock.expectOne('/api/v1/accounts').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(clearSpy).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should not attempt refresh on auth endpoints', () => {
    spyOn(authService, 'isAuthenticated').and.returnValue(true);
    const refreshSpy = spyOn(authService, 'refresh');
    http.post('/api/v1/auth/login', {}).subscribe({ error: () => {} });

    httpMock.expectOne('/api/v1/auth/login').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(refreshSpy).not.toHaveBeenCalled();
  });
});
