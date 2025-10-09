import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, from, switchMap } from 'rxjs';
import { TransformationScriptDTO } from '../../models/transformation-script.model';
import { ConfigService } from '../config.service';

@Injectable({ providedIn: 'root' })
export class ScriptService {
  constructor(private readonly http: HttpClient, private readonly config: ConfigService) {}

  getByTransformationId(
    transformationId: number | string
  ): Observable<TransformationScriptDTO> {
    return from(this.config.getSyncNodeBaseUrl()).pipe(
      switchMap(baseUrl =>
        this.http.get<TransformationScriptDTO>(
          `${baseUrl}/api/config/transformation/${transformationId}/script`
        )
      )
    );
  }
}
