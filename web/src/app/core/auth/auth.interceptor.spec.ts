import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authService: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting()
      ]
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
  });

  afterEach(() => {
    httpMock.verify();
    authService.logout();
  });

  it('should not attach Authorization header when there is no token', () => {
    spyOn(authService, 'token').and.returnValue(null);

    http.get('/api/qualquer').subscribe();

    const request = httpMock.expectOne('/api/qualquer');
    expect(request.request.headers.has('Authorization')).toBeFalse();
    request.flush({});
  });

  it('should attach Bearer token when authenticated', () => {
    spyOn(authService, 'token').and.returnValue('jwt-abc');

    http.get('/api/qualquer').subscribe();

    const request = httpMock.expectOne('/api/qualquer');
    expect(request.request.headers.get('Authorization')).toBe('Bearer jwt-abc');
    request.flush({});
  });
});
