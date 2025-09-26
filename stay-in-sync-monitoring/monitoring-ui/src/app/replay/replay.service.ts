import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ReplayExecuteResponseDTO } from './models/replay.model';

@Injectable({
  providedIn: 'root',
})
export class ReplayService {
  private http = inject(HttpClient);
  private baseUrl = 'http://localhost:8091';

  executeReplay(snapshotId: string): Observable<ReplayExecuteResponseDTO> {
    return this.http.post<ReplayExecuteResponseDTO>(
      `${this.baseUrl}/monitoring/replay/execute/snapshot/${snapshotId}`,
      {}
    );
  }
}
