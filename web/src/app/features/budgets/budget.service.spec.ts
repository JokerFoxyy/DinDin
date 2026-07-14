import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { BudgetService } from './budget.service';

describe('BudgetService', () => {
  let service: BudgetService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(BudgetService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should request the report with month param', () => {
    service.report('2026-07').subscribe();

    const request = httpMock.expectOne((r) => r.url === '/api/v1/budgets');
    expect(request.request.params.get('month')).toBe('2026-07');
    request.flush([]);
  });

  it('should create, update amount and delete a budget', () => {
    service.create({ categoryId: 'c1', month: '2026-07', amount: 500 }).subscribe();
    const post = httpMock.expectOne('/api/v1/budgets');
    expect(post.request.method).toBe('POST');
    post.flush({});

    service.updateAmount('b1', { amount: 700 }).subscribe();
    const put = httpMock.expectOne('/api/v1/budgets/b1');
    expect(put.request.method).toBe('PUT');
    put.flush({});

    service.delete('b1').subscribe();
    const del = httpMock.expectOne('/api/v1/budgets/b1');
    expect(del.request.method).toBe('DELETE');
    del.flush(null);
  });
});
