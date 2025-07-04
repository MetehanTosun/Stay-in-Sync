// src/app/services/api-endpoint-query-param-value.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiEndpointQueryParamValueDTO } from '../models/api-endpoint-query-param-value.dto';

@Injectable({
  providedIn: 'root',
})
export class ApiEndpointQueryParamValueService {
  /** Base path of the backend resource */
  private readonly baseUrl = '/api/config/request-configuration';

  constructor(private http: HttpClient) {}

  /**
   * Creates a new query-param-value for the given request configuration.
   * @param requestConfigId ID of the request-configuration
   * @param dto the payload (all fields except `id`)
   */
  create(
    requestConfigId: number,
    dto: Omit<ApiEndpointQueryParamValueDTO, 'id'>
  ): Observable<void> {
    return this.http.post<void>(
      `${this.baseUrl}/${requestConfigId}/query-param-value`,
      dto
    );
  }

  /**
   * Fetches all query-param-values for the given request configuration.
   */
  getAll(requestConfigId: number): Observable<ApiEndpointQueryParamValueDTO[]> {
    return this.http.get<ApiEndpointQueryParamValueDTO[]>(
      `${this.baseUrl}/${requestConfigId}/query-param-value`
    );
  }

  /**
   * Deletes a query-param-value by its ID.
   */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/query-param-value/${id}`
    );
  }

  /**
   * Fully replaces an existing query-param-value.
   * @param id the ID of the query-param-value to replace
   * @param dto the payload (all fields except `id`)
   */
  update(
    id: number,
    dto: Omit<ApiEndpointQueryParamValueDTO, 'id'>
  ): Observable<void> {
    return this.http.put<void>(
      `${this.baseUrl}/query-param-value/${id}`,
      dto
    );
  }
}