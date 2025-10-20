import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  ReplayExecuteRequestDTO,
  ReplayExecuteResponseDTO,
} from '../../models/replay.model';

/**
 * ReplayService
 * --------------
 * Provides methods for executing transformation replays via the backend API.
 *
 * This service acts as the UI bridge to the replay subsystem, allowing users
 * to send transformation code, source data, and generated SDK snippets to the
 * backend for sandboxed execution (handled by ReplayExecutor on the server).
 *
 * Key responsibilities:
 * - Send replay execution requests using {@link executeReplay}.
 * - Wrap backend responses as observables for reactive handling in Angular.
 *
 * @see ReplayExecuteRequestDTO for the request structure.
 * @see ReplayExecuteResponseDTO for the response structure.
 *
 * @author Mohammed-Ammar Hassnou
 */
@Injectable({
  providedIn: 'root',
})
export class ReplayService {
  /** Injected HttpClient instance */
  private readonly http = inject(HttpClient);

  /**
   * Trigger a transformation replay execution.
   *
   * Sends a {@link ReplayExecuteRequestDTO} payload to the backend endpoint
   * `/api/replay/execute`, initiating a sandboxed JavaScript replay execution.
   * The backend responds with a {@link ReplayExecuteResponseDTO} containing the
   * replay result, including output data, captured variables, and error info.
   *
   * @param dto Data Transfer Object containing the replay parameters (script name,
   *             JavaScript code, source data, and generated SDK).
   * @returns Observable emitting the backend's {@link ReplayExecuteResponseDTO}.
   */
  executeReplay(
    dto: ReplayExecuteRequestDTO
  ): Observable<ReplayExecuteResponseDTO> {
    return this.http.post<ReplayExecuteResponseDTO>('api/replay/execute', dto);
  }
}
