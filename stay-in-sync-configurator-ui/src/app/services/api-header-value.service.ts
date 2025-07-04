import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiHeaderValueDTO } from '../models/api-header-value.dto';

@Injectable({ providedIn: 'root' })
export class ApiHeaderValueService {
  private baseUrl = '/api/config/request-configuration';

  constructor(private http: HttpClient) {}

  /** POST create */
  create(requestConfigId: number, dto: ApiHeaderValueDTO): Observable<void> {
    return this.http.post<void>(
      `${this.baseUrl}/${requestConfigId}/request-header`,
      dto
    );
  }

  /** GET all */
  getAll(requestConfigId: number): Observable<ApiHeaderValueDTO[]> {
    return this.http.get<ApiHeaderValueDTO[]>(
      `${this.baseUrl}/${requestConfigId}/request-header`
    );
  }

  /** GET one */
  getById(id: number): Observable<ApiHeaderValueDTO> {
    return this.http.get<ApiHeaderValueDTO>(
      `${this.baseUrl}/request-header/${id}`
    );
  }

  /** DELETE */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/request-header/${id}`
    );
  }

  /** PUT update */
  update(id: number, dto: ApiHeaderValueDTO): Observable<void> {
    return this.http.put<void>(
      `${this.baseUrl}/request-header/${id}`,
      dto
    );
  }
}