import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { GoalService } from './goal.service';

describe('GoalService', () => {
  let service: GoalService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(GoalService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should list, create, update and delete a goal', () => {
    service.list().subscribe((result) => expect(result).toEqual([]));
    httpMock.expectOne({ url: '/api/v1/goals', method: 'GET' }).flush([]);

    service.create({ name: 'Reserva', targetAmount: 12000, targetDate: '2026-12-01' })
      .subscribe((result) => expect(result).toEqual({} as never));
    httpMock.expectOne({ url: '/api/v1/goals', method: 'POST' }).flush({});

    service.update('g1', { name: 'Reserva maior', targetAmount: 20000, targetDate: '2027-01-01' })
      .subscribe((result) => expect(result).toEqual({} as never));
    httpMock.expectOne({ url: '/api/v1/goals/g1', method: 'PUT' }).flush({});

    service.delete('g1').subscribe((result) => expect(result).toBeNull());
    httpMock.expectOne({ url: '/api/v1/goals/g1', method: 'DELETE' }).flush(null);
  });

  it('should list, create and delete contributions', () => {
    service.listContributions('g1').subscribe((result) => expect(result).toEqual([]));
    httpMock.expectOne({ url: '/api/v1/goals/g1/contributions', method: 'GET' }).flush([]);

    service.createContribution('g1', { month: '2026-07', amount: 800 })
      .subscribe((result) => expect(result).toEqual({} as never));
    httpMock.expectOne({ url: '/api/v1/goals/g1/contributions', method: 'POST' }).flush({});

    service.deleteContribution('g1', 'c1').subscribe((result) => expect(result).toBeNull());
    httpMock.expectOne({ url: '/api/v1/goals/g1/contributions/c1', method: 'DELETE' }).flush(null);
  });
});
