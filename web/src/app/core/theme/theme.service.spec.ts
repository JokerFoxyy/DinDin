import { TestBed } from '@angular/core/testing';

import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  let service: ThemeService;

  beforeEach(() => {
    localStorage.removeItem('poupito.theme');
    document.documentElement.setAttribute('data-theme', 'light');
    TestBed.configureTestingModule({});
    service = TestBed.inject(ThemeService);
  });

  afterEach(() => {
    localStorage.removeItem('poupito.theme');
    document.documentElement.setAttribute('data-theme', 'light');
  });

  it('should read the initial theme from the DOM', () => {
    expect(service.theme()).toBe('light');
  });

  it('should toggle between light and dark, updating the DOM and localStorage', () => {
    service.toggle();

    expect(service.theme()).toBe('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(localStorage.getItem('poupito.theme')).toBe('dark');

    service.toggle();

    expect(service.theme()).toBe('light');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    expect(localStorage.getItem('poupito.theme')).toBe('light');
  });

  it('should set an explicit theme', () => {
    service.set('dark');

    expect(service.theme()).toBe('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });
});
