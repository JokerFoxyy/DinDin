import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';

type Mode = 'login' | 'register';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly mode = signal<Mode>('login');
  readonly errorMessage = signal<string | null>(null);
  readonly loading = signal(false);

  readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(10)]]
  });

  toggleMode(): void {
    this.mode.set(this.mode() === 'login' ? 'register' : 'login');
    this.errorMessage.set(null);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { email, password } = this.form.getRawValue();
    const request$ =
      this.mode() === 'login'
        ? this.authService.login(email, password)
        : this.authService.register(email, password);

    this.loading.set(true);
    this.errorMessage.set(null);
    request$.subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.errorMessage.set(this.messageFor(error));
      }
    });
  }

  private messageFor(error: HttpErrorResponse): string {
    if (error.status === 401) {
      return 'Email ou senha inválidos';
    }
    if (error.status === 409) {
      return 'Email já cadastrado';
    }
    return 'Erro ao comunicar com o servidor. Tente novamente.';
  }
}
