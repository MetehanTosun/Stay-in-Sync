import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SourceSystemDto } from '../models/source-system.model';
import { SourceSystemEndpointDto } from '../models/source-system-endpoint.dto';
import { DiscoveredEndpointDto } from '../models/discovered-endpoint.model';



@Injectable({ providedIn: 'root' })
export class SourceSystemApiService {
  private readonly baseUrl = '/api/source-systems';

  constructor(private http: HttpClient) {}

  // JSON-Only Create (schickt ein reines SourceSystemDto)
  create(dto: SourceSystemDto): Observable<SourceSystemDto> {
    return this.http.post<SourceSystemDto>(this.baseUrl, dto);
  }

  /**
   * Create a source system via multipart/form-data, including file upload.
   */
  createFormData(formData: FormData): Observable<SourceSystemDto> {
    return this.http.post<SourceSystemDto>(this.baseUrl, formData);
  }

  getAll(): Observable<SourceSystemDto[]> {
    return this.http.get<SourceSystemDto[]>(this.baseUrl);
  }

  // Multipart-Upload der OpenAPI-Spec-Datei
  uploadSpecFile(id: number, file: File): Observable<void> {
    const fd = new FormData();
    fd.append('file', file);               
    return this.http.post<void>(
      `${this.baseUrl}/${id}/upload-openapi`,
      fd
    );
  }

  // OpenAPI-Spec per URL setzen
  setSpecUrl(id: number, url: string): Observable<void> {
    const fd = new FormData();
    fd.append('openApiSpecUrl', url);       
    return this.http.post<void>(
      `${this.baseUrl}/${id}/upload-openapi`,
      fd
    );
  }

  setSubmodelLink(id: number, link: string): Observable<void> {
    const fd = new FormData();
    fd.append('openApiSpecUrl', link);
    return this.http.post<void>(
      `${this.baseUrl}/${id}/upload-openapi`,
      fd
    );
  }

  // Schritte 2+3 …
  listEndpoints(sourceId: number): Observable<SourceSystemEndpointDto[]> {
    return this.http.get<SourceSystemEndpointDto[]>(`${this.baseUrl}/${sourceId}/endpoints`);
  }
  

  extractEndpointSchema(sourceId: number, endpointId: number): Observable<any> {
    return this.http.post<any>(
      `${this.baseUrl}/${sourceId}/endpoints/${endpointId}/extract`,
      {}
    );
  }

  createEndpoint(sourceId: number, endpoint: any): Observable<SourceSystemEndpointDto> {
    return this.http.post<SourceSystemEndpointDto>(
      `${this.baseUrl}/${sourceId}/endpoints`,
      endpoint
    );
  }

  /**
   * Discover available endpoints from the stored OpenAPI spec.
   */
  discoverEndpoints(sourceId: number): Observable<DiscoveredEndpointDto[]> {
    return this.http.get<DiscoveredEndpointDto[]>(`${this.baseUrl}/${sourceId}/discover`);
  }
}