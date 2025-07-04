import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateSourceSystemDTO }   from '../models/create-source-system.dto';
import { SourceSystemDTO }         from '../models/source-system.dto';

@Injectable({ providedIn: 'root' })
export class SourceSystemService {
  private readonly baseUrl = '/api/config/source-system';

  constructor(private http: HttpClient) {}

  /**
   * GET /api/config/source-system
   * Returns all source systems
   */
  getAll(): Observable<SourceSystemDTO[]> {
    return this.http.get<SourceSystemDTO[]>(this.baseUrl);
  }

  /**
   * GET /api/config/source-system/{id}
   * Returns one source system by ID (404 if not found)
   */
  getById(id: number): Observable<SourceSystemDTO> {
    return this.http.get<SourceSystemDTO>(`${this.baseUrl}/${id}`);
  }

  /**
   * POST /api/config/source-system
   * Creates a new source system.
   * Returns the full HttpResponse so you can read the Location header.
   */
  create(dto: CreateSourceSystemDTO): Observable<HttpResponse<void>> {
    return this.http.post<void>(
      this.baseUrl,
      dto,
      { observe: 'response' }
    );
  }

  /**
   * PUT /api/config/source-system/{id}
   * Fully replaces an existing source system (404 if not found).
   * Returns the updated SourceSystemDto.
   */
  update(id: number, dto: CreateSourceSystemDTO): Observable<SourceSystemDTO> {
    return this.http.put<SourceSystemDTO>(
      `${this.baseUrl}/${id}`,
      dto
    );
  }

  /**
   * DELETE /api/config/source-system/{id}
   * Deletes a source system by ID (404 if not found).
   */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}