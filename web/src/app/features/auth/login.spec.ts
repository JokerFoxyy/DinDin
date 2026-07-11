import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { Login } from './login';
import { AuthService } from '../../core/auth/auth.service';
import { UserResponse } from '../../core/auth/auth.models';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let component: Login;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  const user: UserResponse = { id: 'u1', email: 'victor@dindin.com' };

  function fillForm(email: string, password: string): void {
    component.form.setValue({ email, password });
  }

  beforeEach(async () => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['login', 'register']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create with login mode by default', () => {
    expect(component.mode()).toBe('login');
  });

  it('should not call the service when form is invalid', () => {
    fillForm('nao-e-email', 'curta');

    component.submit();

    expect(authService.login).not.toHaveBeenCalled();
    expect(authService.register).not.toHaveBeenCalled();
  });

  it('should navigate to dashboard when login succeeds', () => {
    authService.login.and.returnValue(of(user));
    fillForm('victor@dindin.com', 'senha-forte-123');

    component.submit();

    expect(authService.login).toHaveBeenCalledWith('victor@dindin.com', 'senha-forte-123');
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should call register when in register mode', () => {
    authService.register.and.returnValue(of(user));
    component.toggleMode();
    fillForm('novo@dindin.com', 'senha-forte-123');

    component.submit();

    expect(authService.register).toHaveBeenCalledWith('novo@dindin.com', 'senha-forte-123');
  });

  it('should show invalid credentials message when login returns 401', () => {
    authService.login.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 401 }))
    );
    fillForm('victor@dindin.com', 'senha-errada-123');

    component.submit();

    expect(component.errorMessage()).toBe('Email ou senha inválidos');
    expect(component.loading()).toBeFalse();
  });

  it('should show duplicate email message when register returns 409', () => {
    authService.register.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 409 }))
    );
    component.toggleMode();
    fillForm('duplicado@dindin.com', 'senha-forte-123');

    component.submit();

    expect(component.errorMessage()).toBe('Email já cadastrado');
  });

  it('should show generic message when server fails', () => {
    authService.login.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500 }))
    );
    fillForm('victor@dindin.com', 'senha-forte-123');

    component.submit();

    expect(component.errorMessage()).toContain('Erro ao comunicar com o servidor');
  });

  it('should clear error message when toggling mode', () => {
    authService.login.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 401 }))
    );
    fillForm('victor@dindin.com', 'senha-errada-123');
    component.submit();

    component.toggleMode();

    expect(component.mode()).toBe('register');
    expect(component.errorMessage()).toBeNull();
  });
});
