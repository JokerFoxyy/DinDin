import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';

import { Shell } from './shell';
import { AuthService } from '../auth/auth.service';
import { UserResponse } from '../auth/auth.models';
import { BudgetService } from '../../features/budgets/budget.service';
import { BudgetReport } from '../../features/budgets/budget.models';

describe('Shell', () => {
  let fixture: ComponentFixture<Shell>;
  let component: Shell;
  let authService: jasmine.SpyObj<AuthService> & { currentUser: ReturnType<typeof signal<UserResponse | null>> };
  let budgetService: jasmine.SpyObj<BudgetService>;

  const overBudget: BudgetReport = {
    id: 'b1', categoryId: 'c1', categoryName: 'Lazer', categoryIcon: '🎮', categoryColor: '#a371f7',
    budgeted: 100, spent: 150, percentage: 150, over: true
  };

  beforeEach(async () => {
    const spy = jasmine.createSpyObj<AuthService>('AuthService',
      ['loadCurrentUser', 'logout', 'clearSession']);
    authService = Object.assign(spy, { currentUser: signal<UserResponse | null>(null) });
    authService.loadCurrentUser.and.returnValue(of({ id: 'uuid-1', email: 'victor@poupito.com' }));
    authService.logout.and.returnValue(of(void 0));

    budgetService = jasmine.createSpyObj<BudgetService>('BudgetService', ['alerts']);
    budgetService.alerts.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [Shell],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: BudgetService, useValue: budgetService }
      ]
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
    expect(items.length).toBe(9);
    expect(labels.join(' ')).toContain('Dashboard');
    expect(labels.join(' ')).toContain('Transações');
    expect(labels.join(' ')).toContain('Faturas');
    expect(labels.join(' ')).toContain('Orçamentos');
    expect(labels.join(' ')).toContain('Importar');
    expect(labels.join(' ')).toContain('Configurações');
  });

  it('should show the user email when loaded', () => {
    authService.currentUser.set({ id: 'uuid-1', email: 'victor@poupito.com' });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.user-email').textContent).toContain('victor@poupito.com');
  });

  it('should not show a budget alert badge when there are no alerts', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.alert-badge')).toBeNull();
  });

  it('should show a budget alert badge with the count when there are over-budget categories', () => {
    budgetService.alerts.and.returnValue(of([overBudget]));

    fixture.detectChanges();

    expect(component.budgetAlertCount()).toBe(1);
    expect(fixture.nativeElement.querySelector('.alert-badge').textContent).toContain('1');
  });

  it('should toggle the mobile sidebar open and closed', () => {
    fixture.detectChanges();

    expect(component.sidebarOpen()).toBeFalse();

    component.toggleSidebar();
    fixture.detectChanges();
    expect(component.sidebarOpen()).toBeTrue();
    expect(fixture.nativeElement.querySelector('aside').classList).toContain('open');

    component.toggleSidebar();
    expect(component.sidebarOpen()).toBeFalse();
  });

  it('should close the sidebar when a nav item is clicked', () => {
    fixture.detectChanges();
    component.toggleSidebar();
    fixture.detectChanges();

    fixture.nativeElement.querySelector('.nav-item').click();

    expect(component.sidebarOpen()).toBeFalse();
  });

  it('should close the sidebar when the backdrop is clicked', () => {
    component.toggleSidebar();
    fixture.detectChanges();

    fixture.nativeElement.querySelector('.sidebar-backdrop').click();

    expect(component.sidebarOpen()).toBeFalse();
  });

  it('should not render the backdrop when the sidebar is closed', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.sidebar-backdrop')).toBeNull();
  });

  it('should logout and navigate to login when logout is clicked', () => {
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    fixture.detectChanges();

    component.logout();

    expect(authService.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should clear session and go to login when loading current user fails', () => {
    authService.loadCurrentUser.and.returnValue(throwError(() => new Error('401')));
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture.detectChanges();

    expect(authService.clearSession).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
