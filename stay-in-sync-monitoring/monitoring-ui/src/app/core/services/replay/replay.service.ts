import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ReplayExecuteRequestDTO,
  ReplayExecuteResponseDTO,
} from '../../models/replay.model';

@Injectable({
  providedIn: 'root',
})
export class ReplayService {
  private http = inject(HttpClient);
  private baseUrl = 'http://localhost:8090';

  executeReplay(
    dto: ReplayExecuteRequestDTO
  ): Observable<ReplayExecuteResponseDTO> {
    return this.http.post<ReplayExecuteResponseDTO>(
      `${this.baseUrl}/api/replay/execute`,
      dto
    );
  }
}
