import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class PrivacyService {
  private readonly http = inject(HttpClient);

  /** Exportação/portabilidade (LGPD): baixa todos os dados do usuário como JSON. */
  exportData(): Observable<Blob> {
    return this.http.get('/api/v1/account/export', { responseType: 'blob' });
  }

  /** Direito de eliminação (LGPD): apaga a conta e todos os dados vinculados. */
  deleteAccount(): Observable<void> {
    return this.http.delete<void>('/api/v1/account');
  }
}
