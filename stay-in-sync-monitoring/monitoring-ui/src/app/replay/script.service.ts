// src/app/replay/script.service.ts
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { TransformationScriptDTO } from './models/transformation-script.model';

@Injectable({ providedIn: 'root' })
export class ScriptService {
  private readonly baseUrl = '/api/config';

  constructor(private http: HttpClient) {}

  getByTransformationId(
    transformationId: number | string
  ): Observable<TransformationScriptDTO> {
    return this.http.get<TransformationScriptDTO>(
      `${this.baseUrl}/transformation/${transformationId}/script`
    );
  }
}
