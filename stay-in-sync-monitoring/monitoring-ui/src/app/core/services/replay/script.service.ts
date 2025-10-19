import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { TransformationScriptDTO } from '../../models/transformation-script.model';
/**
 * ScriptService
 * --------------
 * Provides methods for retrieving transformation scripts from the backend API.
 *
 * This service acts as the data access layer for transformation scripts. It is used
 * throughout the Replay and Snapshot modules to obtain script source code, SDK data,
 * and metadata by transformation ID.
 *
 * Responsibilities:
 * - Retrieve a transformation's TypeScript/JavaScript source via {@link getByTransformationId}.
 * - Encapsulate HTTP access so other components can remain decoupled from backend URLs.
 *
 * @see TransformationScriptDTO for the DTO definition used in responses.
 *
 * @author Mohammed-Ammar Hassnou
 */
@Injectable({ providedIn: 'root' })
export class ScriptService {
  constructor(private readonly http: HttpClient) {}

  /**
   * Fetch a transformation script by its unique transformation ID.
   *
   * Sends an HTTP GET request to `/api/replay/{transformationId}` and returns
   * the transformation script definition as a {@link TransformationScriptDTO}.
   *
   * @param transformationId The numeric or string identifier of the transformation.
   * @returns Observable emitting a {@link TransformationScriptDTO} when the request succeeds.
   */
  getByTransformationId(
    transformationId: number | string
  ): Observable<TransformationScriptDTO> {
    return this.http.get<TransformationScriptDTO>(
      '/api/replay/' + transformationId
    );
  }
}
