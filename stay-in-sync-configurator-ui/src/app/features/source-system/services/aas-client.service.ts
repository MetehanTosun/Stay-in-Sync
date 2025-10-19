import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type AasSystemType = 'source' | 'target';

@Injectable({ providedIn: 'root' })
export class AasClientService {
  constructor(private http: HttpClient) {}

  /** Build base URL for a system type and id. */
  private base(systemType: AasSystemType, systemId: number): string {
    if (systemType === 'source') {
      return `/api/config/source-system/${systemId}/aas`;
    }
    return `/api/config/target-system/${systemId}/aas`;
  }

  /** Test AAS connection. */
  test(systemType: AasSystemType, systemId: number): Observable<any> {
    return this.http.post(`${this.base(systemType, systemId)}/test`, {});
  }

  /** Request snapshot refresh (source systems only). */
  refreshSnapshot(systemType: AasSystemType, systemId: number): Observable<any> {
    if (systemType !== 'source') {
      throw new Error('refreshSnapshot is only supported for source systems');
    }
    return this.http.post(`${this.base(systemType, systemId)}/snapshot/refresh`, {});
  }

  /** List submodels. */
  listSubmodels(systemType: AasSystemType, systemId: number, params?: any): Observable<any> {
    const url = `${this.base(systemType, systemId)}/submodels`;
    return this.http.get(url, { params });
  }

  /** List elements of a submodel. */
  listElements(systemType: AasSystemType, systemId: number, smId: string, depth?: string, parentPath?: string, source?: string): Observable<any> {
    const url = `${this.base(systemType, systemId)}/submodels/${smId}/elements`;
    const params: any = {};
    if (depth) params.depth = depth;
    if (parentPath) params.parentPath = parentPath;
    if (source && systemType === 'source') params.source = source;
    return this.http.get(url, { params });
  }

  /** Get element details. */
  getElement(systemType: AasSystemType, systemId: number, smId: string, path: string, source?: string): Observable<any> {
    const url = `${this.base(systemType, systemId)}/submodels/${smId}/elements/${this.encodePathSegments(path)}`;
    const params: any = {};
    if (source && systemType === 'source') params.source = source;
    return this.http.get(url, { params });
  }

  /** Create submodel. */
  createSubmodel(systemType: AasSystemType, systemId: number, body: any): Observable<any> {
    return this.http.post(`${this.base(systemType, systemId)}/submodels`, body);
  }

  /** Replace submodel. */
  putSubmodel(systemType: AasSystemType, systemId: number, smId: string, body: any): Observable<any> {
    return this.http.put(`${this.base(systemType, systemId)}/submodels/${smId}`, body);
  }

  /** Delete submodel. */
  deleteSubmodel(systemType: AasSystemType, systemId: number, smId: string): Observable<any> {
    return this.http.delete(`${this.base(systemType, systemId)}/submodels/${smId}`);
  }

  /** Create element under a submodel. */
  createElement(systemType: AasSystemType, systemId: number, smId: string, body: any, parentPath?: string): Observable<any> {
    const url = `${this.base(systemType, systemId)}/submodels/${smId}/elements`;
    const params: any = {};
    if (parentPath) params.parentPath = parentPath;
    return this.http.post(url, body, { params });
  }

  /** Replace element content. */
  putElement(systemType: AasSystemType, systemId: number, smId: string, path: string, body: any): Observable<any> {
    return this.http.put(`${this.base(systemType, systemId)}/submodels/${smId}/elements/${this.encodePathSegments(path)}`, body);
  }

  /** Delete element. */
  deleteElement(systemType: AasSystemType, systemId: number, smId: string, path: string): Observable<any> {
    const encodedSmId = this.encodeIdToBase64Url(smId);
    const url = `${this.base(systemType, systemId)}/submodels/${encodedSmId}/elements/${this.encodePathSegments(path)}`;
    return this.http.delete(url);
  }

  /** Patch element value. */
  patchElementValue(systemType: AasSystemType, systemId: number, smId: string, path: string, body: any): Observable<any> {
    return this.http.patch(`${this.base(systemType, systemId)}/submodels/${smId}/elements/${this.encodePathSegments(path)}/value`, body);
  }

  /** Upload an AASX file. */
  uploadAasx(systemType: AasSystemType, systemId: number, file: File): Observable<any> {
    const url = `${this.base(systemType, systemId)}/upload`;
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post(url, form);
  }

  /** Preview an AASX file. */
  previewAasx(systemType: AasSystemType, systemId: number, file: File): Observable<any> {
    const url = `${this.base(systemType, systemId)}/upload/preview`;
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post(url, form);
    
  }

  /** Attach selected content from an uploaded AASX. */
  attachSelectedAasx(systemType: AasSystemType, systemId: number, file: File, selection: any): Observable<any> {
    const url = `${this.base(systemType, systemId)}/upload/attach-selected`;
    const form = new FormData();
    form.append('file', file, file.name);
    form.append('selection', JSON.stringify(selection));
    return this.http.post(url, form);
  }

  /** Convert slash-separated element paths to dot-separated. */
  private encodePathSegments(path: string): string {
    return path.replace(/\//g, '.');
  }

  /** Encode identifier using Base64. */
  private encodeIdToBase64Url(id: string): string {
    if (!id) return id;
    return btoa(id);
  }
}
