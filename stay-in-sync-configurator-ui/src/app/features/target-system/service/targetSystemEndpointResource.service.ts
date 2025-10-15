import { Injectable } from '@angular/core';
import { HttpClient, HttpContext, HttpEvent, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TargetSystemEndpointDTO } from '../models/targetSystemEndpointDTO';
import { CreateTargetSystemEndpointDTO } from '../models/createTargetSystemEndpointDTO';
import { TypeScriptGenerationRequest } from '../../source-system/models/typescriptGenerationRequest';
import { TypeScriptGenerationResponse } from '../../source-system/models/typescriptGenerationResponse';

/**
 * Service responsible for managing Target System endpoints.
 * Provides CRUD operations and TypeScript interface generation
 * for target system endpoint configurations.
 */
@Injectable({ providedIn: 'root' })
export class TargetSystemEndpointResourceService {
  /**
   * Creates a new instance of the TargetSystemEndpointResourceService.
   * @param http Angular HttpClient instance for performing HTTP requests.
   */
  constructor(private http: HttpClient) {}

  /**
   * Retrieves all endpoints associated with a specific Target System.
   * @param targetSystemId ID of the Target System whose endpoints are fetched.
   * @returns Observable emitting an array of TargetSystemEndpointDTO.
   */
  list(targetSystemId: number): Observable<TargetSystemEndpointDTO[]> {
    return this.http.get<TargetSystemEndpointDTO[]>(`/api/config/target-systems/${targetSystemId}/endpoints`);
    }

  /**
   * Creates one or multiple new endpoints for a specific Target System.
   * @param targetSystemId ID of the Target System.
   * @param payload Array of endpoint creation DTOs.
   * @returns Observable emitting the created TargetSystemEndpointDTO objects.
   */
  create(targetSystemId: number, payload: CreateTargetSystemEndpointDTO[]): Observable<TargetSystemEndpointDTO[]> {
    return this.http.post<TargetSystemEndpointDTO[]>(`/api/config/target-systems/${targetSystemId}/endpoints`, payload);
  }

  /**
   * Retrieves details of a specific Target System endpoint by its ID.
   * @param id ID of the endpoint to retrieve.
   * @returns Observable emitting the corresponding TargetSystemEndpointDTO.
   */
  getById(id: number): Observable<TargetSystemEndpointDTO> {
    return this.http.get<TargetSystemEndpointDTO>(`/api/config/target-systems/endpoints/${id}`);
  }

  /**
   * Replaces an existing Target System endpoint configuration.
   * @param id ID of the endpoint to replace.
   * @param dto Updated TargetSystemEndpointDTO data.
   * @returns Observable that completes upon successful replacement.
   */
  replace(id: number, dto: TargetSystemEndpointDTO): Observable<void> {
    return this.http.put<void>(`/api/config/target-systems/endpoints/${id}`, dto);
  }

  /**
   * Deletes a Target System endpoint by its ID.
   * @param id ID of the endpoint to delete.
   * @returns Observable that completes upon successful deletion.
   */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/config/target-systems/endpoints/${id}`);
  }

  /**
   * Generates a TypeScript interface from a given JSON schema for a Target System endpoint.
   * Mirrors functionality from the Source System service.
   * @param endpointId ID of the endpoint for which TypeScript should be generated.
   * @param request Payload containing the JSON schema or generation parameters.
   * @param observe Determines the observation mode (body, response, or events).
   * @param reportProgress Optional flag for reporting upload progress.
   * @param options Additional HTTP context and header configuration.
   * @returns Observable emitting the generated TypeScript code or full response, depending on `observe`.
   */
  public generateTypeScript(endpointId: number, request: TypeScriptGenerationRequest, observe?: 'body', reportProgress?: boolean, options?: {
    httpHeaderAccept?: 'application/json',
    context?: HttpContext
  }): Observable<TypeScriptGenerationResponse>;
  public generateTypeScript(endpointId: number, request: TypeScriptGenerationRequest, observe?: 'response', reportProgress?: boolean, options?: {
    httpHeaderAccept?: 'application/json',
    context?: HttpContext
  }): Observable<HttpResponse<TypeScriptGenerationResponse>>;
  public generateTypeScript(endpointId: number, request: TypeScriptGenerationRequest, observe?: 'events', reportProgress?: boolean, options?: {
    httpHeaderAccept?: 'application/json',
    context?: HttpContext
  }): Observable<HttpEvent<TypeScriptGenerationResponse>>;
  public generateTypeScript(endpointId: number, request: TypeScriptGenerationRequest, observe: any = 'body', reportProgress: boolean = false, options?: {
    httpHeaderAccept?: 'application/json',
    context?: HttpContext
  }): Observable<any> {
    return this.http.request<TypeScriptGenerationResponse>('post', `/api/config/target-systems/endpoints/${endpointId}/generate-typescript`, {
      body: request,
      context: options?.context,
      observe: observe,
      reportProgress: reportProgress,
      headers: {
        'Content-Type': 'application/json',
        'Accept': options?.httpHeaderAccept || 'application/json'
      }
    });
  }
}
