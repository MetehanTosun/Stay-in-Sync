import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SourceSystemEndpointDTO } from '../models/source-system-endpoint.dto';
import { CreateSourceSystemEndpointDTO } from '../models/create-source-system-endpoint.dto';
@Injectable({ providedIn: 'root' })
export class SourceSystemEndpointService {
  private readonly base = '/api/config/source-system';

  constructor(private http: HttpClient) {}

  /**
   * POST /{sourceSystemId}/endpoint
   * Creates one or more endpoints for the given source system.
   * Returns the array of created SourceSystemEndpointDto.
   */
  createMany(
    sourceSystemId: number,
    endpoints: CreateSourceSystemEndpointDTO[]
  ): Observable<SourceSystemEndpointDTO[]> {
    return this.http.post<SourceSystemEndpointDTO[]>(
      `${this.base}/${sourceSystemId}/endpoint`,
      endpoints
    );
  }
  /**
   * GET /{sourceSystemId}/endpoint
   * Lists all endpoints for a given source system.
   */
  listBySourceSystem(
    sourceSystemId: number
  ): Observable<SourceSystemEndpointDTO[]> {
    return this.http.get<SourceSystemEndpointDTO[]>(
      `${this.base}/${sourceSystemId}/endpoint`
    );
  }

  /**
   * GET /endpoint/{id}
   * Fetches a single endpoint by its ID.
   */
  getById(id: number): Observable<SourceSystemEndpointDTO> {
    return this.http.get<SourceSystemEndpointDTO>(
      `${this.base}/endpoint/${id}`
    );
  }

  /**
   * PUT /endpoint/{id}
   * Fully replaces the endpoint with the given DTO.
   * Returns nothing (204 No Content).
   */
  update(
    id: number,
    dto: SourceSystemEndpointDTO
  ): Observable<void> {
    return this.http.put<void>(
      `${this.base}/endpoint/${id}`,
      dto
    );
  }

  /**
   * DELETE /endpoint/{id}
   * Deletes the endpoint (204 No Content).
   */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(
      `${this.base}/endpoint/${id}`
    );
  }
}