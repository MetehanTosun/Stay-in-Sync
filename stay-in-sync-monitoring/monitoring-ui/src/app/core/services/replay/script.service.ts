import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TransformationScriptDTO } from '../../models/transformation-script.model';
/**
 * ScriptService
 *
 * Provides methods to fetch transformation scripts from the backend API.
 * Uses the ConfigService for potential configuration dependencies.
 */
@Injectable({ providedIn: 'root' })
export class ScriptService {
  constructor(
    private readonly http: HttpClient,
  ) {}

  /**
   * Fetches the transformation script by transformation ID.
   *
   * @param transformationId The ID of the transformation (number or string).
   * @returns Observable emitting a TransformationScriptDTO object.
   */
  getByTransformationId(
    transformationId: number | string
  ): Observable<TransformationScriptDTO> {
    return this.http.get<TransformationScriptDTO>('/api/replay/' + transformationId);
  }
}
