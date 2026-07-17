import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { TransactionService } from './transaction.service';

describe('TransactionService', () => {
  let service: TransactionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(TransactionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should list with month and pagination params only when no filters', () => {
    service.list('2026-07').subscribe();

    const request = httpMock.expectOne((r) => r.url === '/api/v1/transactions');
    expect(request.request.params.get('month')).toBe('2026-07');
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.has('accountId')).toBeFalse();
    request.flush({ content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 });
  });

  it('should include filter params when provided', () => {
    service.list('2026-07', { accountId: 'a1', categoryId: 'c1', type: 'EXPENSE' }, 2, 10).subscribe();

    const request = httpMock.expectOne((r) => r.url === '/api/v1/transactions');
    expect(request.request.params.get('accountId')).toBe('a1');
    expect(request.request.params.get('categoryId')).toBe('c1');
    expect(request.request.params.get('type')).toBe('EXPENSE');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('10');
    request.flush({ content: [], page: 2, size: 10, totalElements: 0, totalPages: 0 });
  });

  it('should include q and tag params when provided', () => {
    service.list('2026-07', { q: 'padaria', tag: 'viagem' }).subscribe();

    const request = httpMock.expectOne((r) => r.url === '/api/v1/transactions');
    expect(request.request.params.get('q')).toBe('padaria');
    expect(request.request.params.get('tag')).toBe('viagem');
    request.flush({ content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 });
  });

  it('should create, update and delete transactions', () => {
    const payload = {
      description: 'Padaria', amount: 31.73, date: '2026-07-09',
      type: 'EXPENSE' as const, accountId: 'a1', categoryId: 'c1', tags: ['viagem']
    };

    service.create(payload).subscribe();
    const post = httpMock.expectOne('/api/v1/transactions');
    expect(post.request.method).toBe('POST');
    post.flush({});

    service.update('t1', payload).subscribe();
    const put = httpMock.expectOne('/api/v1/transactions/t1');
    expect(put.request.method).toBe('PUT');
    put.flush({});

    service.delete('t1').subscribe();
    const del = httpMock.expectOne('/api/v1/transactions/t1');
    expect(del.request.method).toBe('DELETE');
    del.flush(null);
  });

  it('should create with installments when provided', () => {
    service.create({
      description: 'Notebook', amount: 500, date: '2026-07-09', type: 'EXPENSE',
      accountId: 'a1', categoryId: 'c1', tags: [], installments: 6
    }).subscribe();

    const post = httpMock.expectOne('/api/v1/transactions');
    expect((post.request.body as { installments: number }).installments).toBe(6);
    post.flush({});
  });

  it('should include the scope param when deleting with a scope', () => {
    service.delete('t1', 'group').subscribe();

    const del = httpMock.expectOne((r) => r.url === '/api/v1/transactions/t1');
    expect(del.request.params.get('scope')).toBe('group');
    del.flush(null);
  });

  it('should request the export as a blob with filters and format', () => {
    service.export('2026-07', { tag: 'viagem' }, 'xlsx').subscribe((blob) => expect(blob).toBeTruthy());

    const request = httpMock.expectOne((r) => r.url === '/api/v1/transactions/export');
    expect(request.request.params.get('month')).toBe('2026-07');
    expect(request.request.params.get('tag')).toBe('viagem');
    expect(request.request.params.get('format')).toBe('xlsx');
    expect(request.request.responseType).toBe('blob');
    request.flush(new Blob(['data']));
  });
});
