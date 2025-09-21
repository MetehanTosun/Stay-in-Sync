import { Injectable } from '@angular/core';
import { HttpClient, HttpContext, HttpEvent, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TargetSystemEndpointDTO } from '../models/targetSystemEndpointDTO';
import { CreateTargetSystemEndpointDTO } from '../models/createTargetSystemEndpointDTO';
import { TypeScriptGenerationRequest } from '../../source-system/models/typescriptGenerationRequest';
import { TypeScriptGenerationResponse } from '../../source-system/models/typescriptGenerationResponse';

@Injectable({ providedIn: 'root' })
export class TargetSystemEndpointResourceService {
  constructor(private http: HttpClient) {}

  list(targetSystemId: number): Observable<TargetSystemEndpointDTO[]> {
    return this.http.get<TargetSystemEndpointDTO[]>(`/api/config/target-systems/${targetSystemId}/endpoints`);
    }

  create(targetSystemId: number, payload: CreateTargetSystemEndpointDTO[]): Observable<TargetSystemEndpointDTO[]> {
    return this.http.post<TargetSystemEndpointDTO[]>(`/api/config/target-systems/${targetSystemId}/endpoints`, payload);
  }

  getById(id: number): Observable<TargetSystemEndpointDTO> {
    return this.http.get<TargetSystemEndpointDTO>(`/api/config/target-systems/endpoints/${id}`);
  }

  replace(id: number, dto: TargetSystemEndpointDTO): Observable<void> {
    return this.http.put<void>(`/api/config/target-systems/endpoints/${id}`, dto);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/config/target-systems/endpoints/${id}`);
  }

  /**
   * Generate TypeScript interface from JSON schema for target endpoint (mirrors Source service)
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


