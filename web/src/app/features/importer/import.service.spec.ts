import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { ImportService } from './import.service';

describe('ImportService', () => {
  let service: ImportService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ImportService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should post the file with year param on preview', () => {
    const file = new File(['x'], 'planilha.xlsx');

    service.preview(file, 2026).subscribe();

    const request = httpMock.expectOne((r) => r.url === '/api/v1/import/preview');
    expect(request.request.method).toBe('POST');
    expect(request.request.params.get('year')).toBe('2026');
    expect(request.request.body instanceof FormData).toBeTrue();
    request.flush({ rows: [], unmatchedAccounts: [], unmatchedCategories: [] });
  });

  it('should post the file and mapping on commit', () => {
    const file = new File(['x'], 'planilha.xlsx');
    const mapping = { accounts: {}, categories: {} };

    service.commit(file, 2026, mapping).subscribe();

    const request = httpMock.expectOne((r) => r.url === '/api/v1/import/commit');
    expect(request.request.method).toBe('POST');
    expect(request.request.params.get('year')).toBe('2026');
    expect(request.request.body instanceof FormData).toBeTrue();
    request.flush({ transactionsCreated: 0, transactionsSkippedAsDuplicate: 0, accountsCreated: 0, categoriesCreated: 0 });
  });
});
