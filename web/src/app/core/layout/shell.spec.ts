import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';

import { Shell } from './shell';
import { AuthService } from '../auth/auth.service';
import { MeResponse } from '../auth/auth.models';

describe('Shell', () => {
  let fixture: ComponentFixture<Shell>;
  let component: Shell;
  let authService: jasmine.SpyObj<AuthService> & { currentUser: ReturnType<typeof signal<MeResponse | null>> };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<AuthService>('AuthService', ['loadCurrentUser', 'logout']);
    authService = Object.assign(spy, { currentUser: signal<MeResponse | null>(null) });
    authService.loadCurrentUser.and.returnValue(of({ id: 'uuid-1', email: 'victor@dindin.com' }));

    await TestBed.configureTestingModule({
      imports: [Shell],
      providers: [provideRouter([]), { provide: AuthService, useValue: authService }]
    }).compileComponents();

    fixture = TestBed.createComponent(Shell);
    component = fixture.componentInstance;
  });

  it('should load current user on init', () => {
    fixture.detectChanges();

    expect(authService.loadCurrentUser).toHaveBeenCalled();
  });

  it('should render all navigation items', () => {
    fixture.detectChanges();

    const items = fixture.nativeElement.querySelectorAll('.nav-item');
    const labels = Array.from(items).map((el) => (el as HTMLElement).textContent?.trim());
    expect(items.length).toBe(6);
    expect(labels.join(' ')).toContain('Dashboard');
    expect(labels.join(' ')).toContain('Transações');
    expect(labels.join(' ')).toContain('Configurações');
  });

  it('should show the user email when loaded', () => {
    authService.currentUser.set({ id: 'uuid-1', email: 'victor@dindin.com' });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.user-email').textContent).toContain('victor@dindin.com');
  });

  it('should logout and navigate to login when logout is clicked', () => {
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    fixture.detectChanges();

    component.logout();

    expect(authService.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should logout when loading current user fails', () => {
    authService.loadCurrentUser.and.returnValue(throwError(() => new Error('401')));
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture.detectChanges();

    expect(authService.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
