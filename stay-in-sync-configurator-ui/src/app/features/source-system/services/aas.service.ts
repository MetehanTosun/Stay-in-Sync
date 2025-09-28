import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AasArc, AasArcSaveRequest } from '../../script-editor/models/arc.models';
import { AasTargetArcConfiguration, CreateAasTargetArcDTO } from '../../script-editor/models/target-system.models';

@Injectable({ providedIn: 'root' })
export class AasService {
  constructor(private http: HttpClient) {}

  aasTest(sourceSystemId: number): Observable<any> {
    const url = `/api/config/source-system/${sourceSystemId}/aas/test`;
    return this.http.post(url, {});
  }

  refreshSnapshot(sourceSystemId: number): Observable<any> {
    const url = `/api/config/source-system/${sourceSystemId}/aas/snapshot/refresh`;
    return this.http.post(url, {});
  }

  listSubmodels(sourceSystemId: number, source: 'SNAPSHOT' | 'LIVE' = 'SNAPSHOT'): Observable<any> {
    const url = `/api/config/source-system/${sourceSystemId}/aas/submodels`;
    const params = new HttpParams().set('source', source);
    return this.http.get(url, { params });
  }

  listElements(
    sourceSystemId: number,
    submodelId: string,
    options?: { depth?: 'shallow' | 'all'; parentPath?: string; source?: 'SNAPSHOT' | 'LIVE' }
  ): Observable<any> {
    const depth = options?.depth ?? 'shallow';
    const source = options?.source ?? 'SNAPSHOT';
    let params = new HttpParams().set('depth', depth).set('source', source);
    if (options?.parentPath) {
      params = params.set('parentPath', options.parentPath);
    }
    const submodelIdEnc = this.encodeIdToBase64Url(submodelId);
    const url = `/api/config/source-system/${sourceSystemId}/aas/submodels/${submodelIdEnc}/elements`;
    return this.http.get(url, { params });
  }

  createSubmodel(sourceSystemId: number, submodel: any): Observable<any> {
    const url = `/api/config/source-system/${sourceSystemId}/aas/submodels`;
    return this.http.post(url, submodel);
  }

  createElement(
    sourceSystemId: number,
    submodelId: string,
    element: any,
    parentPath?: string
  ): Observable<any> {
    const url = `/api/config/source-system/${sourceSystemId}/aas/submodels/${submodelId}/elements`;
    const params = parentPath ? new HttpParams().set('parentPath', parentPath) : undefined;
    return this.http.post(url, element, { params });
  }

  setPropertyValue(
    sourceSystemId: number,
    submodelId: string,
    elementPath: string,
    value: any
  ): Observable<any> {
    const url = `/api/config/source-system/${sourceSystemId}/aas/submodels/${submodelId}/elements/${elementPath}/value`;
    // Ensure JSON content type; quote strings as JSON
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    const body = (typeof value === 'string') ? JSON.stringify(value) : value;
    return this.http.patch(url, body, { headers });
  }

  deleteSubmodel(sourceSystemId: number, submodelId: string): Observable<any> {
    const url = `/api/config/source-system/${sourceSystemId}/aas/submodels/${submodelId}`;
    return this.http.delete(url);
  }

  deleteElement(
    sourceSystemId: number,
    submodelId: string,
    elementPath: string
  ): Observable<any> {
    const url = `/api/config/source-system/${sourceSystemId}/aas/submodels/${submodelId}/elements/${elementPath}`;
    return this.http.delete(url);
  }

  encodeIdToBase64Url(id: string): string {
    if (!id) return id;
    const b64 = typeof window !== 'undefined' && (window as any).btoa
      ? (window as any).btoa(unescape(encodeURIComponent(id)))
      : id; // fallback: return original
    return b64
      .replace(/=+$/g, '')
      .replace(/\+/g, '-')
      .replace(/\//g, '_');
  }

  createAasArc(dto: AasArcSaveRequest): Observable<AasArc> {
    const url = `/api/config/aas-request-configuration`;
    return this.http.post<AasArc>(url, dto);
  }

  updateAasArc(arcId: number, dto: AasArcSaveRequest): Observable<AasArc> {
    const url = `/api/config/aas-request-configuration/${arcId}`;
    return this.http.put<AasArc>(url, dto);
  }

  deleteAasArc(arcId: number): Observable<void> {
    const url = `/api/config/aas-request-configuration/${arcId}`;
    return this.http.delete<void>(url);
  }

  createAasTargetArc(dto: CreateAasTargetArcDTO): Observable<AasTargetArcConfiguration> {
    return this.http.post<AasTargetArcConfiguration>('/api/config/aas-target-request-configuration', dto);
  }
  
  deleteAasTargetArc(arcId: number): Observable<void> {
    return this.http.delete<void>(`/api/config/aas-target-request-configuration/${arcId}`);
  }
}


