import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AasArc, AasArcSaveRequest } from '../../script-editor/models/arc.models';
import { AasTargetArcConfiguration, CreateAasTargetArcDTO } from '../../script-editor/models/target-system.models';

@Injectable({ providedIn: 'root' })
export class AasService {
  constructor(private http: HttpClient) {}

  /**
   * Test connection to an AAS backend for a given source system.
   */
  aasTest(sourceSystemId: number): Observable<any> {
    const url = `/api/config/source-system/${sourceSystemId}/aas/test`;
    return this.http.post(url, {});
  }

  /**
   * Request a snapshot refresh for the given source system.
   */
  refreshSnapshot(sourceSystemId: number): Observable<any> {
    const url = `/api/config/source-system/${sourceSystemId}/aas/snapshot/refresh`;
    return this.http.post(url, {});
  }

  /**
   * Upload an AASX file for the given source system.
   */
  uploadAasx(sourceSystemId: number, file: File): Observable<any> {
    const url = `/api/config/source-system/${sourceSystemId}/aas/upload`;
    const form = new FormData();
    form.append('file', file, file.name);
    form.append('filename', file.name);
    return this.http.post(url, form);
  }

  /**
   * Preview contents of an AASX file without attaching it.
   */
  previewAasx(sourceSystemId: number, file: File): Observable<any> {
    const url = `/api/config/source-system/${sourceSystemId}/aas/upload/preview`;
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post(url, form);
  }

  /**
   * Attach selected submodels/elements from an uploaded AASX file.
   */
  attachSelectedAasx(sourceSystemId: number, file: File, selection: any): Observable<any> {
    const url = `/api/config/source-system/${sourceSystemId}/aas/upload/attach-selected`;
    const form = new FormData();
    form.append('file', file, file.name);
    form.append('selection', JSON.stringify(selection));
    return this.http.post(url, form);
  }

  /**
   * List submodels for a given source system from the specified source (SNAPSHOT or LIVE).
   */
  listSubmodels(sourceSystemId: number, source: 'SNAPSHOT' | 'LIVE' = 'SNAPSHOT'): Observable<any> {
    const url = `/api/config/source-system/${sourceSystemId}/aas/submodels`;
    const params = new HttpParams().set('source', source);
    return this.http.get(url, { params });
  }

  /**
   * List elements for a submodel with optional depth/parentPath/source options.
   */
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

  /**
   * Get a single element details from the specified source (default LIVE).
   */
  getElement(
    sourceSystemId: number,
    submodelId: string,
    idShortPath: string,
    source: 'SNAPSHOT' | 'LIVE' = 'LIVE'
  ): Observable<any> {
    const submodelIdEnc = this.encodeIdToBase64Url(submodelId);
    const pathEnc = this.encodePathSegments(idShortPath);
    const url = `/api/config/source-system/${sourceSystemId}/aas/submodels/${submodelIdEnc}/elements/${pathEnc}`;
    const params = new HttpParams().set('source', source);
    return this.http.get(url, { params });
  }

  /**
   * Create a submodel for the given source system.
   */
  createSubmodel(sourceSystemId: number, submodel: any): Observable<any> {
    const url = `/api/config/source-system/${sourceSystemId}/aas/submodels`;
    return this.http.post(url, submodel);
  }

  /**
   * Create an element under a submodel. submodelId must be Base64-encoded.
   */
  createElement(
    sourceSystemId: number,
    submodelId: string,
    element: any,
    parentPath?: string
  ): Observable<any> {
    const url = `/api/config/source-system/${sourceSystemId}/aas/submodels/${submodelId}/elements`;
    
    const encodedParentPath = parentPath && parentPath.trim() ? this.encodePathSegments(parentPath) : undefined;
    const params = encodedParentPath ? new HttpParams().set('parentPath', encodedParentPath) : undefined;
    
    return this.http.post(url, element, { params });
  }

  /**
   * Set a property value for an element.
   */
  setPropertyValue(
    sourceSystemId: number,
    submodelId: string,
    elementPath: string,
    value: any
  ): Observable<any> {
    const url = `/api/config/source-system/${sourceSystemId}/aas/submodels/${submodelId}/elements/${this.encodePathSegments(elementPath)}/value`;
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    const body = (typeof value === 'string') ? JSON.stringify(value) : value;
    return this.http.patch(url, body, { headers });
  }

  /**
   * Delete a submodel by Base64-encoded identifier.
   */
  deleteSubmodel(sourceSystemId: number, submodelId: string): Observable<any> {
    const url = `/api/config/source-system/${sourceSystemId}/aas/submodels/${submodelId}`;
    return this.http.delete(url);
  }

  /**
   * Delete an element under a submodel by raw IDs (submodelId will be encoded internally).
   */
  deleteElement(
    sourceSystemId: number,
    submodelId: string,
    elementPath: string
  ): Observable<any> {
    const encodedSubmodelId = this.encodeIdToBase64Url(submodelId);
    const encodedPath = this.encodePathSegments(elementPath);
    const url = `/api/config/source-system/${sourceSystemId}/aas/submodels/${encodedSubmodelId}/elements/${encodedPath}`;
    
    return this.http.delete(url);
  }

  /**
   * Encode an identifier to Base64 for backend compatibility.
   */
  encodeIdToBase64Url(id: string): string {
    if (!id) return id;
    return btoa(id);
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

  /**
   * Convert slash-separated element paths to dot-separated notation.
   */
  private encodePathSegments(path: string): string {
    return path.replace(/\//g, '.');
  }
}


