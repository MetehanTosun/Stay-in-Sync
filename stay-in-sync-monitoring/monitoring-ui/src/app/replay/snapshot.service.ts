// src/app/replay/snapshot.service.ts
import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SnapshotDTO } from './models/snapshot.model';

@Injectable({ providedIn: 'root' })
export class SnapshotService {
  private http = inject(HttpClient);
  private baseUrl = '/api/snapshots';

  getById(id: string): Observable<SnapshotDTO> {
    return this.http.get<SnapshotDTO>(`${this.baseUrl}/${id}`);
  }
}
