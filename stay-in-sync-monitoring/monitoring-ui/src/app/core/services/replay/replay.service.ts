import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, from, switchMap } from 'rxjs';
import { ConfigService } from '../config.service';
import {
  ReplayExecuteRequestDTO,
  ReplayExecuteResponseDTO,
} from '../../models/replay.model';

@Injectable({
  providedIn: 'root',
})
export class ReplayService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(ConfigService);

  executeReplay(
    dto: ReplayExecuteRequestDTO
  ): Observable<ReplayExecuteResponseDTO> {
    return from(this.config.getSyncNodeBaseUrl()).pipe(
      switchMap(baseUrl =>
        this.http.post<ReplayExecuteResponseDTO>(
          `${baseUrl}/api/replay/execute`,
          dto
        )
      )
    );
  }
}
