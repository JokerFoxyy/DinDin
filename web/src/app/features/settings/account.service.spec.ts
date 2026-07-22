import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AccountService } from './account.service';
import { Account } from './settings.models';

describe('AccountService', () => {
  let service: AccountService;
  let httpMock: HttpTestingController;

  const account: Account = { id: '1', name: 'Nubank', type: 'CHECKING' };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AccountService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should list accounts', () => {
    let result: Account[] = [];
    service.list().subscribe((accounts) => (result = accounts));

    const request = httpMock.expectOne('/api/v1/accounts');
    expect(request.request.method).toBe('GET');
    request.flush([account]);

    expect(result).toEqual([account]);
  });

  it('should create an account', () => {
    service.create({ name: 'Nubank', type: 'CHECKING' }).subscribe();

    const request = httpMock.expectOne('/api/v1/accounts');
    expect(request.request.method).toBe('POST');
    expect(request.request.body.name).toBe('Nubank');
    request.flush(account);
  });

  it('should update an account', () => {
    service.update('1', { name: 'Nubank UV', type: 'CHECKING' }).subscribe();

    const request = httpMock.expectOne('/api/v1/accounts/1');
    expect(request.request.method).toBe('PUT');
    request.flush(account);
  });

  it('should delete an account', () => {
    service.delete('1').subscribe();

    const request = httpMock.expectOne('/api/v1/accounts/1');
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
  });
});
