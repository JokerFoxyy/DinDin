import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';

import { AuthService } from './auth.service';

/**
 * Envia os cookies httpOnly (withCredentials) e, ao receber 401 numa chamada de app,
 * tenta renovar a sessão via /auth/refresh uma vez e repete a requisição. Se o refresh
 * falhar, limpa o estado local e manda para o login.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const withCredentials = request.clone({ withCredentials: true });
  const isAuthEndpoint = request.url.includes('/api/v1/auth/');

  return next(withCredentials).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || isAuthEndpoint || !authService.isAuthenticated()) {
        return throwError(() => error);
      }
      return authService.refresh().pipe(
        switchMap(() => next(withCredentials)),
        catchError((refreshError) => {
          authService.clearSession();
          router.navigate(['/login']);
          return throwError(() => refreshError);
        })
      );
    })
  );
};
