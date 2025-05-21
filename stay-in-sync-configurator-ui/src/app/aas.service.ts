import { Injectable } from '@angular/core';
// biome-ignore lint/style/useImportType: <explanation>
import { HttpClient } from '@angular/common/http';
// biome-ignore lint/style/useImportType: <explanation>
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AasService {
  private baseUrl = '/aasServer'; // über Proxy erreichbar

  constructor(private http: HttpClient) {}

  // Holt alle registrierten AAS-Instanzen
  getAllShells(): Observable<any> {
    return this.http.get(`${this.baseUrl}/shells`);
  }

  // Holt Submodelle einer bestimmten AAS
  getSubmodels(aasId: string): Observable<any> {
    return this.http.get(`${this.baseUrl}/shells/${aasId}/submodels`);
  }

  // Holt den Wert eines konkreten Elements
  getSubmodelValue(submodelId: string, elementId: string): Observable<any> {
    return this.http.get(`/submodels/${submodelId}/submodelElements/${elementId}/value`);
  }
}
