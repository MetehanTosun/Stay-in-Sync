import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

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

  uploadAasx(sourceSystemId: number, file: File): Observable<any> {
    const url = `/api/config/source-system/${sourceSystemId}/aas/upload`;
    const form = new FormData();
    form.append('file', file, file.name);
    form.append('filename', file.name);
    return this.http.post(url, form);
  }

  previewAasx(sourceSystemId: number, file: File): Observable<any> {
    const url = `/api/config/source-system/${sourceSystemId}/aas/upload/preview`;
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post(url, form);
  }

  attachSelectedAasx(sourceSystemId: number, file: File, selection: any): Observable<any> {
    const url = `/api/config/source-system/${sourceSystemId}/aas/upload/attach-selected`;
    const form = new FormData();
    form.append('file', file, file.name);
    form.append('selection', JSON.stringify(selection));
    return this.http.post(url, form);
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
    
    console.log('[AasService] listElements: FRONTEND REQUEST', {
      sourceSystemId,
      submodelId,
      submodelIdEnc,
      options,
      url,
      params: {
        depth: params.get('depth'),
        parentPath: params.get('parentPath'),
        source: params.get('source')
      }
    });
    
    return this.http.get(url, { params });
  }

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
    // submodelId is already Base64-encoded, use it directly
    const url = `/api/config/source-system/${sourceSystemId}/aas/submodels/${submodelId}/elements`;
    
    // Encode parentPath to dot-separated format for BaSyx compatibility
    const encodedParentPath = parentPath && parentPath.trim() ? this.encodePathSegments(parentPath) : undefined;
    const params = encodedParentPath ? new HttpParams().set('parentPath', encodedParentPath) : undefined;
    
    console.log('[AasService] createElement: API call', {
      url,
      urlLength: url.length,
      sourceSystemId,
      submodelId,
      parentPath,
      encodedParentPath,
      element
    });
    
    return this.http.post(url, element, { params });
  }

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

  deleteSubmodel(sourceSystemId: number, submodelId: string): Observable<any> {
    const url = `/api/config/source-system/${sourceSystemId}/aas/submodels/${submodelId}`;
    return this.http.delete(url);
  }

  deleteElement(
    sourceSystemId: number,
    submodelId: string,
    elementPath: string
  ): Observable<any> {
    // BaSyx API expects UTF8-BASE64-URL-encoded submodelIdentifier
    const encodedSubmodelId = this.encodeIdToBase64Url(submodelId);
    const encodedPath = this.encodePathSegments(elementPath);
    const url = `/api/config/source-system/${sourceSystemId}/aas/submodels/${encodedSubmodelId}/elements/${encodedPath}`;
    
    console.log('[AasService] deleteElement: API call', {
      url,
      urlLength: url.length,
      sourceSystemId,
      submodelId,
      encodedSubmodelId,
      elementPath,
      encodedPath,
      pathLength: elementPath.length,
      encodedPathLength: encodedPath.length
    });
    
    // Log the full URL for debugging
    console.log('[AasService] deleteElement: Full URL', url);
    
    return this.http.delete(url);
  }

  encodeIdToBase64Url(id: string): string {
    if (!id) return id;
    
    // Backend expects normal Base64 with padding, not Base64-URL
    return btoa(id);
  }

  private encodePathSegments(path: string): string {
    // BaSyx expects dot-separated paths, not slash-separated
    const result = path.replace(/\//g, '.');
    
    console.log('[AasService] encodePathSegments: Converting slash to dot', {
      original: path,
      result: result,
      originalLength: path.length,
      resultLength: result.length
    });
    
    return result;
  }
}


