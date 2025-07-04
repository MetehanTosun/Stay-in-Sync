import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApiHeaderDto } from '../models/api-header.dto';
import { CreateApiHeaderDto } from '../models/create-api-header.dto';

@Injectable({ providedIn: 'root' })
export class ApiHeaderService {
  private readonly baseUrl = '/api/config/sync-system';

  constructor(private http: HttpClient) {}

  /**
   * POST /api/config/sync-system/{syncSystemId}/request-header
   * Creates a new API header for the given syncSystemId.
   * Returns the full HttpResponse so you can read the Location header.
   */
  create(
    syncSystemId: number,
    dto: CreateApiHeaderDto
  ): Observable<HttpResponse<void>> {
    const url = `${this.baseUrl}/${syncSystemId}/request-header`;
    return this.http.post<void>(url, dto, { observe: 'response' });
  }

  /**
   * GET /api/config/sync-system/{syncSystemId}/request-header
   * Lists all API headers for the specified sync system.
   */
  getAll(syncSystemId: number): Observable<ApiHeaderDto[]> {
    const url = `${this.baseUrl}/${syncSystemId}/request-header`;
    return this.http.get<ApiHeaderDto[]>(url);
  }

  /**
   * GET /api/config/sync-system/request-header/{id}
   * Retrieves one API header by its ID.
   */
  getById(id: number): Observable<ApiHeaderDto> {
    const url = `${this.baseUrl}/request-header/${id}`;
    return this.http.get<ApiHeaderDto>(url);
  }

  /**
   * PUT /api/config/sync-system/request-header/{id}
   * Replaces an existing API header.  
   * Returns nothing on success (204).
   */
  update(dto: ApiHeaderDto): Observable<void> {
    const url = `${this.baseUrl}/request-header/${dto.id}`;
    return this.http.put<void>(url, dto);
  }

  /**
   * DELETE /api/config/sync-system/request-header/{id}
   * Deletes an API header.
   */
  delete(id: number): Observable<void> {
    const url = `${this.baseUrl}/request-header/${id}`;
    return this.http.delete<void>(url);
  }
}