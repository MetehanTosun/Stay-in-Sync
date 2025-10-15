import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable} from 'rxjs';
import {
  ReplayExecuteRequestDTO,
  ReplayExecuteResponseDTO,
} from '../../models/replay.model';

@Injectable({
  providedIn: 'root',
})
export class ReplayService {
  private readonly http = inject(HttpClient);

  executeReplay(
    dto: ReplayExecuteRequestDTO
  ): Observable<ReplayExecuteResponseDTO> {
    return this.http.post<ReplayExecuteResponseDTO>('api/replay/execute', dto);
  }
}
