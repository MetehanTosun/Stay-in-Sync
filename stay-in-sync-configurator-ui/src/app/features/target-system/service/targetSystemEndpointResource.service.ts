import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TargetSystemEndpointDTO } from '../models/targetSystemEndpointDTO';
import { CreateTargetSystemEndpointDTO } from '../models/createTargetSystemEndpointDTO';
import { TypeScriptGenerationRequest } from '../../source-system/models/typescriptGenerationRequest';
import { TypeScriptGenerationResponse } from '../../source-system/models/typescriptGenerationResponse';

@Injectable({ providedIn: 'root' })
export class TargetSystemEndpointResourceService {
  constructor(private http: HttpClient) {}

  list(targetSystemId: number): Observable<TargetSystemEndpointDTO[]> {
    return this.http.get<TargetSystemEndpointDTO[]>(`/api/target-systems/${targetSystemId}/endpoint`);
    }

  create(targetSystemId: number, payload: CreateTargetSystemEndpointDTO[]): Observable<TargetSystemEndpointDTO[]> {
    return this.http.post<TargetSystemEndpointDTO[]>(`/api/target-systems/${targetSystemId}/endpoint`, payload);
  }

  getById(id: number): Observable<TargetSystemEndpointDTO> {
    return this.http.get<TargetSystemEndpointDTO>(`/api/target-systems/endpoint/${id}`);
  }

  replace(id: number, dto: TargetSystemEndpointDTO): Observable<void> {
    return this.http.put<void>(`/api/target-systems/endpoint/${id}`, dto);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/target-systems/endpoint/${id}`);
  }

  generateTypeScript(endpointId: number, request: TypeScriptGenerationRequest): Observable<TypeScriptGenerationResponse> {
    return this.http.post<TypeScriptGenerationResponse>(`/api/target-systems/endpoint/${endpointId}/generate-typescript`, request);
  }
}


