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
  UpdateTransformationRequestConfigurationDTO,
} from '../../features/script-editor/models/target-system.models';

/**
 * @description Defines the payload for creating or updating a transformation script.
 */
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

/**
 * @description Defines the structure for information about where an ARC is being used.
 */
export interface ArcUsageInfo {
  scriptId: number;
  scriptName: string;
}

/**
 * @description
 * The ScriptEditorService acts as the primary Data Access Layer (DAL) for the script editor feature.
 * It is responsible for all HTTP communication with the backend API, encapsulating endpoint URLs
 * and data transformation logic. This service provides methods for managing Source ARCs, Target ARCs,
 * Source/Target Systems, and the transformation script itself.
 */
@Injectable({
  providedIn: 'root',
})
export class ScriptEditorService {
  /** @private The base URL for all API calls. */
  private readonly API_URL = '/api';
  private http = inject(HttpClient);

  constructor() {}

  // --- Source System and ARC Methods ---

  /**
   * @description Fetches a list of all available source system names.
   * Used for initial editor setup.
   * @returns An `Observable` that emits an array of strings.
   */
  getSourceSystemNames(): Observable<string[]> {
    return this.http.get<string[]>(
      `${this.API_URL}/config/source-system/systemNames`
    );
  }

  /**
   * @description Fetches the full configuration details for a single Source ARC, including
   * its saved parameter and header values. Used when opening the ARC wizard in "edit" mode.
   * @param arcId The ID of the ARC to fetch.
   * @returns An `Observable` that emits the complete `ApiRequestConfiguration`.
   */
  getArcDetails(arcId: number): Observable<ApiRequestConfiguration> {
    return this.http.get<ApiRequestConfiguration>(
      `${this.API_URL}/config/source-system/endpoint/request-configuration/${arcId}`
    );
  }

  /**
   * @description Sends a request to delete a Source ARC from the backend.
   * @param arcId The ID of the ARC to delete.
   * @returns An `Observable<void>` that completes upon successful deletion.
   */
  deleteArc(arcId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.API_URL}/config/source-system/endpoint/request-configuration/${arcId}`
    );
  }

  /**
   * @description Checks where a specific ARC is being used across different scripts.
   * Used to warn the user before deleting an ARC.
   * @param arcId The ID of the ARC to check.
   * @returns An `Observable` that emits an array of `ArcUsageInfo` objects.
   * @todo Replace with the actual API endpoint once it's available.
   */
  checkArcUsage(arcId: number): Observable<ArcUsageInfo[]> {
    // TODO: Replace with the actual API endpoint once it's available.
    return of([]);
  }

  /**
   * @description Sends a temporary ARC configuration to the backend for a live test call.
   * This is used in the ARC wizard to validate the configuration and generate the response schema.
   * @param request The `ArcTestCallRequest` payload.
   * @returns An `Observable` of the `ArcTestCallResponse`.
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
   * @description Saves a new or updated `ApiRequestConfiguration` to the backend.
   * It intelligently uses PUT for updates (if `request.id` exists) and POST for creations.
   * @param request The `ArcSaveRequest` payload.
   * @returns An `Observable` of the saved `ApiRequestConfiguration`, including its new ID if created.
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
   * @description Fetches all REST-based ARCs associated with a specific Source System.
   * Used for on-demand loading of ARCs in the "Data Sources" panel.
   * @param systemId The ID of the source system.
   * @returns An `Observable` that emits an array of `ApiRequestConfiguration`.
   */
  getArcsForSourceSystem(
    systemId: number
  ): Observable<ApiRequestConfiguration[]> {
    return this.http.get<ApiRequestConfiguration[]>(
      `${this.API_URL}/config/source-system/${systemId}/request-configuration/`
    );
  }

  /**
   * @description Fetches a list of all available source systems.
   * @returns An `Observable` that emits an array of `SourceSystem` objects.
   */
  getSourceSystems(): Observable<SourceSystem[]> {
    return this.http.get<SourceSystem[]>(
      `${this.API_URL}/config/source-system`
    );
  }

  /**
   * @description Fetches all endpoints for a specific source system.
   * @param systemId The ID of the source system.
   * @returns An `Observable` that emits an array of `SourceSystemEndpoint`.
   */
  getEndpointsForSourceSystem(
    systemId: number
  ): Observable<SourceSystemEndpoint[]> {
    return this.http.get<SourceSystemEndpoint[]>(
      `${this.API_URL}/config/source-system/${systemId}/endpoint`
    );
  }

  // --- Target System and ARC Methods ---

  /**
   * @description Fetches a list of all available target systems.
   * @returns An `Observable` that emits an array of `TargetSystem` objects.
   */
  getTargetSystems(): Observable<TargetSystem[]> {
    return this.http.get<TargetSystem[]>(
      `${this.API_URL}/config/target-systems`
    );
  }

  /**
   * @description Fetches all REST-based Target ARCs for a given Target System.
   * @param targetSystemId The ID of the target system.
   * @returns An `Observable` that emits an array of `TargetArcConfiguration`.
   */
  getArcsByTargetSystem(
    targetSystemId: number
  ): Observable<TargetArcConfiguration[]> {
    return this.http.get<TargetArcConfiguration[]>(
      `${this.API_URL}/config/target-arcs/by-system/${targetSystemId}`
    );
  }

  /**
   * @description Fetches endpoint suggestions for a target system, likely from an OpenAPI spec.
   * @param targetSystemId The ID of the target system.
   * @returns An `Observable` that emits an array of `EndpointSuggestion`.
   */
  getEndpointSuggestionsForTargetSystem(
    targetSystemId: number
  ): Observable<EndpointSuggestion[]> {
    return this.http.get<EndpointSuggestion[]>(
      `${this.API_URL}/config/target-systems/${targetSystemId}/endpoints`
    );
  }

  /**
   * @description Creates a new REST-based Target ARC.
   * @param dto The `CreateTargetArcDTO` payload.
   * @returns An `Observable` of the created `TargetArcConfiguration`.
   */
  createArc(dto: CreateTargetArcDTO): Observable<TargetArcConfiguration> {
    return this.http.post<TargetArcConfiguration>(
      `${this.API_URL}/config/target-arcs`,
      dto
    );
  }

  /**
   * @description Fetches all active Target ARCs (both REST and AAS) for a given transformation.
   * @param transformationId The ID of the transformation.
   * @returns An `Observable` that emits an array of `AnyTargetArc`.
   */
  getActiveAnyArcsForTransformation(
    transformationId: number
  ): Observable<AnyTargetArc[]> {
    return this.http.get<AnyTargetArc[]>(
      `${this.API_URL}/config/transformation/${transformationId}/target-arcs`
    );
  }

  /**
   * @description Updates the set of Target ARCs linked to a transformation and returns the new type definitions.
   * @param transformationId The ID of the transformation to update.
   * @param targetArcIds A DTO containing the lists of REST and AAS Target ARC IDs.
   * @returns An `Observable` of the newly generated `TypeDefinitionsResponse` for the Monaco editor.
   */
  updateTransformationTargetArcs(
    transformationId: number,
    targetArcIds: UpdateTransformationRequestConfigurationDTO
  ): Observable<TypeDefinitionsResponse> {
    return this.http.put<TypeDefinitionsResponse>(
      `${this.API_URL}/config/transformation/${transformationId}/target-arcs`,
      targetArcIds
    );
  }

  /**
   * @description Fetches the TypeScript type definitions for all Target ARCs linked to a transformation.
   * @param transformationId The ID of the transformation.
   * @returns An `Observable` of the `TypeDefinitionsResponse`.
   */
  getTargetTypeDefinitions(
    transformationId: number
  ): Observable<TypeDefinitionsResponse> {
    return this.http.get<TypeDefinitionsResponse>(
      `${this.API_URL}/config/transformation/${transformationId}/target-type-definitions`
    );
  }

  // --- AAS Specific Methods ---

  /**
   * @description Creates a new AAS-based Target ARC.
   * @param dto The `CreateAasTargetArcDTO` payload.
   * @returns An `Observable` of the created `AasTargetArcConfiguration`.
   */
  createAasTargetArc(
    dto: CreateAasTargetArcDTO
  ): Observable<AasTargetArcConfiguration> {
    return this.http.post<AasTargetArcConfiguration>(
      `${this.API_URL}/config/aas-target-request-configuration`,
      dto
    );
  }

  /**
   * @description Deletes an AAS-based Target ARC.
   * @param arcId The ID of the AAS Target ARC to delete.
   * @returns An `Observable<void>` that completes on success.
   */
  deleteAasTargetArc(arcId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.API_URL}/config/aas-target-request-configuration/${arcId}`
    );
  }

  /**
   * @description Fetches submodel descriptions for an AAS-based system.
   * @param targetSystemId The ID of the target/source system.
   * @returns An `Observable` that emits an array of `SubmodelDescription`.
   * @todo The URL currently points to 'source-system'. This should be updated to a generic
   * or target-system specific endpoint if the backend implementation differs.
   */
  getSubmodelsForTargetSystem(
    targetSystemId: number
  ): Observable<SubmodelDescription[]> {
    const url = `/api/config/source-system/${targetSystemId}/aas/submodels`;
    const params = new HttpParams().set('source', 'SNAPSHOT');
    return this.http.get<SubmodelDescription[]>(url, { params });
  }

  /**
   * @description Updates an existing AAS-based Target ARC.
   * @param arcId The ID of the AAS Target ARC to update.
   * @param dto The `CreateAasTargetArcDTO` payload with the new data.
   * @returns An `Observable` of the updated `AasTargetArcConfiguration`.
   */
  updateAasTargetArc(
    arcId: number,
    dto: CreateAasTargetArcDTO
  ): Observable<AasTargetArcConfiguration> {
    return this.http.put<AasTargetArcConfiguration>(
      `${this.API_URL}/config/aas-target-request-configuration/${arcId}`,
      dto
    );
  }

  // --- Wizard and Context Methods ---

  /**
   * @description Fetches all necessary context data for the Source ARC wizard, including
   * parameter definitions and header definitions. It combines multiple HTTP calls into one.
   * @param systemId The ID of the source system.
   * @param endpointId The ID of the source system endpoint.
   * @returns An `Observable` that emits the complete `ArcWizardContextData`.
   * @todo The API endpoints for params and headers have inconsistent naming conventions
   * ('endpoint' vs 'sync-system'). These should be streamlined in the backend.
   */
  getArcWizardContextData(
    systemId: number,
    endpointId: number
  ): Observable<ArcWizardContextData> {
    const params$ = this.http.get<ApiEndpointParamDTO[]>(
      `${this.API_URL}/config/endpoint/${endpointId}/query-param`
    );

    const headers$ = this.http.get<ApiHeaderDefinition[]>(
      `${this.API_URL}/config/sync-system/${systemId}/request-header`
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
            type: (dto.schemaType?.toLowerCase() ?? 'string') as
              | 'string'
              | 'number'
              | 'integer'
              | 'boolean'
              | 'array',
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

  /**
   * @description Fetches context data related to a specific sync job execution.
   * @param jobId The ID of the sync job.
   * @returns An `Observable` of `SyncJobContextData`.
   */
  getSyncJobContext(jobId: string): Observable<SyncJobContextData> {
    return this.http.get<SyncJobContextData>(
      `${this.API_URL}/config/sync-job/${jobId}/`
    );
  }

  // --- Script Management Methods ---

  /**
   * @description Fetches the saved script for a given transformation.
   * @param transformationId The ID of the transformation.
   * @returns An `Observable` that emits the `ScriptPayload` or `null`.
   * @todo Properly implement lazy loading of the script if this is a performance concern.
   */
  getSavedScript(transformationId: string): Observable<ScriptPayload | null> {
    return this.http.get<ScriptPayload>(
      `${this.API_URL}/config/transformation/${transformationId}/transformation-script`
    );
  }

  /**
   * @description Saves (creates or updates) the script for a transformation.
   * @param transformationId The ID of the transformation.
   * @param payload The `ScriptPayload` to save.
   * @returns An `Observable` of the saved `ScriptPayload`.
   */
  saveScriptForTransformation(
    transformationId: number,
    payload: ScriptPayload
  ): Observable<ScriptPayload> {
    return this.http.put<ScriptPayload>(
      `${this.API_URL}/config/transformation/${transformationId}/script`,
      payload
    );
  }

  /**
   * @description Fetches the script for a given transformation.
   * @param transformationId The ID of the transformation.
   * @returns An `Observable` of the `ScriptPayload`.
   */
  getScriptForTransformation(
    transformationId: number
  ): Observable<ScriptPayload> {
    return this.http.get<ScriptPayload>(
      `${this.API_URL}/config/transformation/${transformationId}/script`
    );
  }
}
