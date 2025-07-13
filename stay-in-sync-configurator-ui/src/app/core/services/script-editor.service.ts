import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { forkJoin, map, Observable } from 'rxjs';
import { SyncJobContextData } from '../../features/script-editor/sync-job-context-panel/sync-job-context-panel.component';

import {
  ApiEndpointParamDTO,
  ApiHeaderDefinition,
  ApiRequestConfiguration,
  ArcSaveRequest,
  ArcTestCallRequest,
  ArcTestCallResponse,
  ArcWizardContextData,
  EndpointParameterDefinition,
} from '../../features/script-editor/models/arc.models';
import {
  SourceSystem,
  SourceSystemEndpoint,
} from '../../features/source-system/models/source-system.models';

export interface ScriptPayload {
  id?: string;
  name: string | null | undefined;
  typescriptCode: string;
  javascriptCode?: string;
  hash: string;
  requiredArcAliases?: string[];
}

@Injectable({
  providedIn: 'root',
})
export class ScriptEditorService {
  //TODO placeholder api endpoint baseurl
  private readonly API_URL = '/api';
  private http = inject(HttpClient);

  constructor() {}

  getSourceSystemNames(): Observable<string[]> {
    return this.http.get<string[]>(
      `${this.API_URL}/config/source-system/systemNames`
    );
  }

  /**
   * Sends a temporary ARC configuration to the backend for a live test call.
   * @param request The configuration to test.
   * @returns An observable of the test call result.
   */
  testArcConfiguration(
    request: ArcTestCallRequest
  ): Observable<ArcTestCallResponse> {
    return this.http.post<ArcTestCallResponse>(
      `${this.API_URL}/config/arc-test-call`,
      request
    );
  }

  /**
   * Saves a new or updated ApiRequestConfiguration to the backend.
   * @param request The ARC data to save.
   * @returns An observable of the saved ARC, including its new ID if created.
   */
  saveArcConfiguration(
    request: ArcSaveRequest
  ): Observable<ApiRequestConfiguration> {
    if (request.id) {
      return this.http.put<ApiRequestConfiguration>(
        `${this.API_URL}/config/source-system/endpoint/request-configuration/${request.id}`,
        request
      );
    }
    return this.http.post<ApiRequestConfiguration>(
      `${this.API_URL}/config/source-system/endpoint/${request.endpointId}/request-configuration/`,
      request
    );
  }

  /**
   * Fetches all ARCs associated with a specific Source System.
   * This is used for lazy-loading types into the editor.
   * @param systemId The ID of the source system.
   * @returns An observable array of ARCs.
   */
  getArcsForSourceSystem(
    systemId: number
  ): Observable<ApiRequestConfiguration[]> {
    return this.http.get<ApiRequestConfiguration[]>(
      `${this.API_URL}/config/source-system/${systemId}/request-configuration/`
    );
  }

  /**
   * Fetches a list of all available source systems.
   */
  getSourceSystems(): Observable<SourceSystem[]> {
    return this.http.get<SourceSystem[]>(
      `${this.API_URL}/config/source-system`
    );
  }

  /**
   * Fetches all endpoints for a specific source system.
   * @param systemId The ID of the source system.
   */
  getEndpointsForSourceSystem(
    systemId: number
  ): Observable<SourceSystemEndpoint[]> {
    return this.http.get<SourceSystemEndpoint[]>(
      `${this.API_URL}/config/source-system/${systemId}/endpoint`
    );
  }

  getArcWizardContextData(
    systemId: number,
    endpointId: number
  ): Observable<ArcWizardContextData> {
    const params$ = this.http.get<ApiEndpointParamDTO[]>(
      `${this.API_URL}/config/endpoint/${endpointId}/query-param`
    ); // TODO GET PROPER ENDPOINT

    const headers$ = this.http.get<ApiHeaderDefinition[]>(
      `${this.API_URL}/config/sync-system/${systemId}/request-header` // TODO STREAMLINE API CONVENTIONS (sync/source)
    );

    return forkJoin([params$, headers$]).pipe(
      map(([paramDtos, headerDefs]) => {
        const pathParams: EndpointParameterDefinition[] = [];
        const queryParamDefinitions: EndpointParameterDefinition[] = [];

        paramDtos.forEach((dto) => {
          const definition: EndpointParameterDefinition = {
            name: dto.paramName,
            in: dto.queryParamType.toLowerCase() as 'path' | 'query',
            description: dto.description || '',
            required: dto.required || false,
            options: dto.values || [],
          };

          if (definition.in === 'path') {
            pathParams.push(definition);
          } else {
            queryParamDefinitions.push(definition);
          }
        });

        const wizardData: ArcWizardContextData = {
          pathParams: pathParams,
          queryParamDefinitions: queryParamDefinitions,
          headerDefinitions: headerDefs,
        };

        return wizardData;
      })
    );
  }

  getSyncJobContext(jobId: string): Observable<SyncJobContextData> {
    return this.http.get<SyncJobContextData>(
      `${this.API_URL}/config/sync-job/${jobId}/`
    );
  }

  // TODO: Properly lazyload existing script
  getSavedScript(transformationId: string): Observable<ScriptPayload | null> {
    return this.http.get<ScriptPayload>(
      `${this.API_URL}/config/transformation/${transformationId}/transformation-script`
    );
  }

  saveScriptForTransformation(
    transformationId: number,
    payload: ScriptPayload
  ): Observable<ScriptPayload> {
    return this.http.put<ScriptPayload>(`${this.API_URL}/config/transformation/${transformationId}/script`, payload);
  }

  getScriptForTransformation(
    transformationId: number
  ): Observable<ScriptPayload> {
    return this.http.get<ScriptPayload>(`${this.API_URL}/config/transformation/${transformationId}/script`);
  }
}
