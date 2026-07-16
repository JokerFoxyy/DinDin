import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { InvestmentService } from './investment.service';

describe('InvestmentService', () => {
  let service: InvestmentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(InvestmentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should list, create, update and delete an investment', () => {
    service.list().subscribe((result) => expect(result).toEqual([]));
    httpMock.expectOne({ url: '/api/v1/investments', method: 'GET' }).flush([]);

    service.create({ name: 'Tesouro Selic', assetClass: 'RENDA_FIXA', institution: 'NuInvest' })
      .subscribe((result) => expect(result).toEqual({} as never));
    httpMock.expectOne({ url: '/api/v1/investments', method: 'POST' }).flush({});

    service.update('i1', { name: 'Tesouro IPCA', institution: 'Rico' })
      .subscribe((result) => expect(result).toEqual({} as never));
    httpMock.expectOne({ url: '/api/v1/investments/i1', method: 'PUT' }).flush({});

    service.delete('i1').subscribe((result) => expect(result).toBeNull());
    httpMock.expectOne({ url: '/api/v1/investments/i1', method: 'DELETE' }).flush(null);
  });

  it('should list, create and delete entries', () => {
    service.listEntries('i1').subscribe((result) => expect(result).toEqual([]));
    httpMock.expectOne({ url: '/api/v1/investments/i1/entries', method: 'GET' }).flush([]);

    service.createEntry('i1', { date: '2026-01-05', type: 'APORTE', amount: 500, balanceAfter: null })
      .subscribe((result) => expect(result).toEqual({} as never));
    httpMock.expectOne({ url: '/api/v1/investments/i1/entries', method: 'POST' }).flush({});

    service.deleteEntry('i1', 'e1').subscribe((result) => expect(result).toBeNull());
    httpMock.expectOne({ url: '/api/v1/investments/i1/entries/e1', method: 'DELETE' }).flush(null);
  });

  it('should fetch the report', () => {
    service.report().subscribe((result) => expect(result).toEqual({ investments: [], byClass: [] }));

    httpMock.expectOne({ url: '/api/v1/investments/report', method: 'GET' }).flush({ investments: [], byClass: [] });
  });

  it('should fetch the cdi accumulated series with from/to params', () => {
    service.cdi('2026-01-01', '2026-01-31').subscribe((result) => expect(result).toEqual([]));

    const request = httpMock.expectOne((r) => r.url === '/api/v1/investments/cdi');
    expect(request.request.params.get('from')).toBe('2026-01-01');
    expect(request.request.params.get('to')).toBe('2026-01-31');
    request.flush([]);
  });
});
