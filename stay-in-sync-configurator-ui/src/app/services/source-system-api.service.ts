import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SourceSystemDto } from '../models/source-system.model';

@Injectable({ providedIn: 'root' })
export class SourceSystemApiService {
  private readonly baseUrl = '/api/source-systems';

  constructor(private http: HttpClient) {}

  create(dto: SourceSystemDto): Observable<SourceSystemDto> {
    return this.http.post<SourceSystemDto>(this.baseUrl, dto);
  }

  getAll(): Observable<SourceSystemDto[]> {
    return this.http.get<SourceSystemDto[]>(this.baseUrl);
  }

  uploadSpecFile(id: number, file: File): Observable<void> {
    const fd = new FormData();
    fd.append('file', file);
    return this.http.post<void>(`${this.baseUrl}/${id}/upload-openapi`, fd);
  }

  setSpecUrl(id: number, url: string): Observable<void> {
    const fd = new FormData();
    fd.append('url', url);
    return this.http.post<void>(`${this.baseUrl}/${id}/upload-openapi`, fd);
  }

  setSubmodelLink(id: number, link: string): Observable<void> {
    const fd = new FormData();
    fd.append('url', link); // backend treats 'url' as spec or link
    return this.http.post<void>(`${this.baseUrl}/${id}/upload-openapi`, fd);
  }

  // später für Schritt 2:
  listEndpoints(sourceId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/${sourceId}/endpoints`);
  }

  extractEndpointSchema(sourceId: number, endpointId: number): Observable<any> {
    return this.http.post<any>(
      `${this.baseUrl}/${sourceId}/endpoints/${endpointId}/extract`,
      {}
    );
  }
}