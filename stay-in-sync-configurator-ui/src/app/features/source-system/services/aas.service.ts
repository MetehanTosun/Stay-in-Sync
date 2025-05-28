// biome-ignore lint/style/useImportType: <explanation>
import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
// biome-ignore lint/style/useImportType: <explanation>
import { Observable } from "rxjs/internal/Observable";

@Injectable({ providedIn: 'root' })
export class AasService {
  private baseUrl = '/aasServer'; // über Proxy erreichbar

  constructor(private http: HttpClient) {}

  /** Holt alle registrierten AAS-Instanzen */
  getAll(): Observable<{ id: string; name: string }[]> {
    return this.http.get<{ id: string; name: string }[]>(`${this.baseUrl}/shells`);
  }

  /** Holt Submodelle einer bestimmten AAS */
  getSubmodels(aasId: string): Observable<{ id: string; name: string }[]> {
    return this.http.get<{ id: string; name: string }[]>(`${this.baseUrl}/shells/${aasId}/submodels`);
  }

  /** Holt den Wert eines konkreten Elements */
  getSubmodelValue(
    submodelId: string,
    elementId: string
  ): Observable<{ value: unknown }> {
    return this.http.get<{ value: unknown }>(
      `/submodels/${submodelId}/submodelElements/${elementId}/value`
    );
  }
}