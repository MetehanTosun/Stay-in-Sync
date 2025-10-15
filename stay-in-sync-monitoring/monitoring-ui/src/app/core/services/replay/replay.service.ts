import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ReplayExecuteRequestDTO,
  ReplayExecuteResponseDTO,
} from '../../models/replay.model';

/**
 * ReplayService
 *
 * Provides methods to trigger replay executions via the backend API.
 * Uses Angular's modern `inject()` API for dependency injection.
 */
@Injectable({
  providedIn: 'root',
})
export class ReplayService {
  /** Injected HttpClient instance */
  private readonly http = inject(HttpClient);

  /**
   * Executes a replay for a given replay request.
   *
   * @param dto Data transfer object containing the replay parameters.
   * @returns Observable emitting the backend's replay execution response.
   */
  executeReplay(
    dto: ReplayExecuteRequestDTO
  ): Observable<ReplayExecuteResponseDTO> {
    return this.http.post<ReplayExecuteResponseDTO>('api/replay/execute', dto);
  }
}
