// src/app/services/request-configuration.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateRequestConfigurationDto } from '../models/create-request-configuration.dto';
import { GetRequestConfigurationDto } from '../models/get-request-configuration.dto';

@Injectable({
  providedIn: 'root',
})
export class RequestConfigurationService {
  /** Base URL for all request-configuration endpoints */
  private readonly baseUrl = '/api/config/source-system';

  constructor(private readonly http: HttpClient) {}

  /**
   * Create a new request-configuration for the given endpoint.
   * @param endpointId ID of the source-system endpoint
   * @param dto payload for creation
   */
  createForEndpoint(
    endpointId: number,
    dto: CreateRequestConfigurationDto
  ): Observable<void> {
    return this.http.post<void>(
      `${this.baseUrl}/endpoint/${endpointId}/request-configuration`,
      dto
    );
  }

  /**
   * Fetch all request-configurations for a given endpoint.
   */
  getAllByEndpoint(endpointId: number): Observable<GetRequestConfigurationDto[]> {
    return this.http.get<GetRequestConfigurationDto[]>(
      `${this.baseUrl}/endpoint/${endpointId}/request-configuration`
    );
  }

  /**
   * Fetch all request-configurations for a given source-system.
   */
  getAllBySourceSystem(sourceSystemId: number): Observable<GetRequestConfigurationDto[]> {
    return this.http.get<GetRequestConfigurationDto[]>(
      `${this.baseUrl}/${sourceSystemId}/request-configuration`
    );
  }

  /**
   * Fetch a single request-configuration by its ID.
   */
  getById(id: number): Observable<GetRequestConfigurationDto> {
    return this.http.get<GetRequestConfigurationDto>(
      `${this.baseUrl}/endpoint/request-configuration/${id}`
    );
  }

  /**
   * Update (replace) an existing request-configuration.
   * @param id ID of the request-configuration to update
   * @param dto updated payload (use the same shape as create)
   */
  update(id: number, dto: CreateRequestConfigurationDto): Observable<void> {
    return this.http.put<void>(
      `${this.baseUrl}/endpoint/request-configuration/${id}`,
      dto
    );
  }

  /**
   * Delete a request-configuration by its ID.
   */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/endpoint/request-configuration/${id}`
    );
  }
}