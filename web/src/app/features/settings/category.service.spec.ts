import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { CategoryService } from './category.service';
import { Category } from './settings.models';

describe('CategoryService', () => {
  let service: CategoryService;
  let httpMock: HttpTestingController;

  const category: Category = { id: '1', name: 'Mercado', icon: '🛒', color: '#3fb950', kind: 'EXPENSE' };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(CategoryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should list categories', () => {
    let result: Category[] = [];
    service.list().subscribe((categories) => (result = categories));

    const request = httpMock.expectOne('/api/v1/categories');
    expect(request.request.method).toBe('GET');
    request.flush([category]);

    expect(result).toEqual([category]);
  });

  it('should create a category', () => {
    service.create({ name: 'Mercado', icon: '🛒', color: '#3fb950', kind: 'EXPENSE' }).subscribe();

    const request = httpMock.expectOne('/api/v1/categories');
    expect(request.request.method).toBe('POST');
    request.flush(category);
  });

  it('should update a category', () => {
    service.update('1', { name: 'Supermercado', icon: '🛒', color: '#d29922', kind: 'EXPENSE' }).subscribe();

    const request = httpMock.expectOne('/api/v1/categories/1');
    expect(request.request.method).toBe('PUT');
    request.flush(category);
  });

  it('should delete a category', () => {
    service.delete('1').subscribe();

    const request = httpMock.expectOne('/api/v1/categories/1');
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
  });
});
