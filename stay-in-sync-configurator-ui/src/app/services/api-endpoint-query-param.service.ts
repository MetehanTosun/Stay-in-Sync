// src/app/services/api-endpoint-query-param.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiEndpointQueryParamDTO } from '../models/api-endpoint-query-param.dto';

@Injectable({
  providedIn: 'root',
})
export class ApiEndpointQueryParamService {
  /** Base path of the backend resource */
  private readonly baseUrl = '/api/config/endpoint';

  constructor(private http: HttpClient) {}

  /**
   * Create a new query-param for the given endpoint.
   * @param endpointId the ID of the endpoint
   * @param dto all fields except `id`
   */
  create(
    endpointId: number,
    dto: Omit<ApiEndpointQueryParamDTO, 'id'>
  ): Observable<void> {
    return this.http.post<void>(
      `${this.baseUrl}/${endpointId}/query-param`,
      dto
    );
  }

  /**
   * Fetch all query-params for the given endpoint.
   */
  getAll(endpointId: number): Observable<ApiEndpointQueryParamDTO[]> {
    return this.http.get<ApiEndpointQueryParamDTO[]>(
      `${this.baseUrl}/${endpointId}/query-param`
    );
  }

  /**
   * Fetch a single query-param by its ID.
   */
  getById(id: number): Observable<ApiEndpointQueryParamDTO> {
    return this.http.get<ApiEndpointQueryParamDTO>(
      `${this.baseUrl}/query-param/${id}`
    );
  }

  /**
   * Delete a query-param by its ID.
   */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/query-param/${id}`
    );
  }

  /**
   * Fully replace an existing query-param.
   * @param id the ID of the query-param to replace
   * @param dto all fields except `id`
   */
  update(
    id: number,
    dto: Omit<ApiEndpointQueryParamDTO, 'id'>
  ): Observable<void> {
    return this.http.put<void>(
      `${this.baseUrl}/query-param/${id}`,
      dto
    );
  }
}