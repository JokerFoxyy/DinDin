import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { DashboardService } from './dashboard.service';

describe('DashboardService', () => {
  let service: DashboardService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(DashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should request the summary with month param', () => {
    service.summary('2026-07').subscribe();

    const request = httpMock.expectOne((r) => r.url === '/api/v1/dashboard/summary');
    expect(request.request.params.get('month')).toBe('2026-07');
    request.flush({
      income: 0, expense: 0, monthBalance: 0, cumulativeBalance: 0, categorySpend: [], budgetReport: []
    });
  });

  it('should request the annual series with month param', () => {
    service.annual('2026-07').subscribe();

    const request = httpMock.expectOne((r) => r.url === '/api/v1/dashboard/annual');
    expect(request.request.params.get('month')).toBe('2026-07');
    request.flush([]);
  });
});
