import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ImportCommitResult, ImportMapping, ImportPreview } from './import.models';

const API = '/api/v1/import';

@Injectable({ providedIn: 'root' })
export class ImportService {
  private readonly http = inject(HttpClient);

  preview(file: File, year: number): Observable<ImportPreview> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ImportPreview>(`${API}/preview`, form, { params: { year } });
  }

  commit(file: File, year: number, mapping: ImportMapping): Observable<ImportCommitResult> {
    const form = new FormData();
    form.append('file', file);
    form.append('mapping', new Blob([JSON.stringify(mapping)], { type: 'application/json' }));
    return this.http.post<ImportCommitResult>(`${API}/commit`, form, { params: { year } });
  }
}
