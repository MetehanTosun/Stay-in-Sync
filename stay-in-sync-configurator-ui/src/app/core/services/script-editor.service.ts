import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { forkJoin, map, Observable, of } from 'rxjs';
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
  SubmodelDescription,
} from '../../features/script-editor/models/arc.models';
import {
  SourceSystem,
  SourceSystemEndpoint,
} from '../../features/source-system/models/source-system.models';
import {
  AasTargetArcConfiguration,
  AnyTargetArc,
  CreateAasTargetArcDTO,
  CreateTargetArcDTO,
  EndpointSuggestion,
  TargetArcConfiguration,
  TargetSystem,
  TypeDefinitionsResponse,
  UpdateTransformationRequestConfigurationDTO
} from '../../features/script-editor/models/target-system.models';

export interface ScriptPayload {
  id?: string;
  name: string;
  typescriptCode: string;
  javascriptCode?: string;
  requiredArcAliases?: string[];
  status: 'DRAFT' | 'VALIDATED';
  restTargetArcIds: number[];
  aasTargetArcIds: number[];
}

export interface ArcUsageInfo {
  scriptId: number;
  scriptName: string;
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
   * Fetches the full configuration details for a single ARC.
   * @param arcId The ID of the ARC to fetch.
   */
  getArcDetails(arcId: number): Observable<ApiRequestConfiguration> {
    return this.http.get<ApiRequestConfiguration>(
      `${this.API_URL}/config/source-system/endpoint/request-configuration/${arcId}`
    );
  }

  /**
   * Deletes a Source ARC from the backend.
   * @param arcId The ID of the ARC to delete.
   */
  deleteArc(arcId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.API_URL}/config/source-system/endpoint/request-configuration/${arcId}`
    );
  }

  /**
   * Checks where a specific ARC is being used across different scripts.
   * @param arcId The ID of the ARC to check.
   */
  checkArcUsage(arcId: number): Observable<ArcUsageInfo[]> {
    //
    // TODO: Replace with the actual API endpoint once it's available.
    // Example: return this.http.get<ArcUsageInfo[]>(`${this.API_URL}/config/request-configuration/${arcId}/usages`);
    // For now, returning an empty array to allow UI development.
    return of([]);
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

  getTargetSystems(): Observable<TargetSystem[]> {
    return this.http.get<TargetSystem[]>(`${this.API_URL}/config/target-systems`);
  }

  getArcsByTargetSystem(targetSystemId: number): Observable<TargetArcConfiguration[]> {
    return this.http.get<TargetArcConfiguration[]>(`${this.API_URL}/config/target-arcs/by-system/${targetSystemId}`);
  }

  // TODO: Add Endpoint
  getEndpointSuggestionsForTargetSystem(targetSystemId: number): Observable<EndpointSuggestion[]> {
    return this.http.get<EndpointSuggestion[]>(`${this.API_URL}/config/target-systems/${targetSystemId}/endpoints`);
  }

  createArc(dto: CreateTargetArcDTO): Observable<TargetArcConfiguration> {
    return this.http.post<TargetArcConfiguration>(`${this.API_URL}/config/target-arcs`, dto);
  }

  // deprecated
  getActiveArcsForTransformation(transformationId: string): Observable<TargetArcConfiguration[]> {
    return this.http.get<TargetArcConfiguration[]>(`${this.API_URL}/config/transformation/${transformationId}/target-arcs`);
  }

  getActiveAnyArcsForTransformation(transformationId: number): Observable<AnyTargetArc[]> {
    return this.http.get<AnyTargetArc[]>(`${this.API_URL}/config/transformation/${transformationId}/target-arcs`);
  }

  updateTransformationTargetArcs(transformationId: number, targetArcIds: UpdateTransformationRequestConfigurationDTO): Observable<any> {
    return this.http.put(`${this.API_URL}/config/transformation/${transformationId}/target-arcs`, targetArcIds);
  }

  getTargetTypeDefinitions(transformationId: number): Observable<TypeDefinitionsResponse> {
    return this.http.get<TypeDefinitionsResponse>(`${this.API_URL}/config/transformation/${transformationId}/target-type-definitions`);
  }

  createAasTargetArc(dto: CreateAasTargetArcDTO): Observable<AasTargetArcConfiguration> {
    return this.http.post<AasTargetArcConfiguration>(`${this.API_URL}/config/aas-target-request-configuration`, dto);
  }

  deleteAasTargetArc(arcId: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/config/aas-target-request-configuration/${arcId}`);
  }

  // TODO: Update to TargetSystem
  getSubmodelsForTargetSystem(targetSystemId: number): Observable<SubmodelDescription[]> {
    const url = `/api/config/source-system/${targetSystemId}/aas/submodels`;
    const params = new HttpParams().set('source', 'SNAPSHOT');
    return this.http.get<SubmodelDescription[]>(url, { params });
  }

  updateAasTargetArc(arcId: number, dto: CreateAasTargetArcDTO): Observable<AasTargetArcConfiguration> {
    return this.http.put<AasTargetArcConfiguration>(`${this.API_URL}/config/aas-target-request-configuration/${arcId}`, dto);
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
            type: (dto.schemaType?.toLowerCase() ?? 'string') as 'string' | 'number' | 'integer' | 'boolean' | 'array',
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
