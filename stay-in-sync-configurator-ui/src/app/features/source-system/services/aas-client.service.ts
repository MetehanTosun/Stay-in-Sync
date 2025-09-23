import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type AasSystemType = 'source' | 'target';

@Injectable({ providedIn: 'root' })
export class AasClientService {
  constructor(private http: HttpClient) {}

  private base(systemType: AasSystemType, systemId: number): string {
    if (systemType === 'source') {
      return `/api/config/source-system/${systemId}/aas`;
    }
    return `/api/config/target-system/${systemId}/aas`;
  }

  test(systemType: AasSystemType, systemId: number): Observable<any> {
    return this.http.post(`${this.base(systemType, systemId)}/test`, {});
  }

  refreshSnapshot(systemType: AasSystemType, systemId: number): Observable<any> {
    // Only supported for source systems
    if (systemType !== 'source') {
      throw new Error('refreshSnapshot is only supported for source systems');
    }
    return this.http.post(`${this.base(systemType, systemId)}/snapshot/refresh`, {});
  }

  listSubmodels(systemType: AasSystemType, systemId: number, params?: any): Observable<any> {
    const url = `${this.base(systemType, systemId)}/submodels`;
    return this.http.get(url, { params });
  }

  listElements(systemType: AasSystemType, systemId: number, smId: string, depth?: string, parentPath?: string, source?: string): Observable<any> {
    const url = `${this.base(systemType, systemId)}/submodels/${smId}/elements`;
    const params: any = {};
    if (depth) params.depth = depth;
    if (parentPath) params.parentPath = parentPath;
    if (source && systemType === 'source') params.source = source;
    return this.http.get(url, { params });
  }

  getElement(systemType: AasSystemType, systemId: number, smId: string, path: string, source?: string): Observable<any> {
    const url = `${this.base(systemType, systemId)}/submodels/${smId}/elements/${this.encodePathSegments(path)}`;
    const params: any = {};
    if (source && systemType === 'source') params.source = source;
    return this.http.get(url, { params });
  }

  createSubmodel(systemType: AasSystemType, systemId: number, body: any): Observable<any> {
    return this.http.post(`${this.base(systemType, systemId)}/submodels`, body);
  }

  putSubmodel(systemType: AasSystemType, systemId: number, smId: string, body: any): Observable<any> {
    return this.http.put(`${this.base(systemType, systemId)}/submodels/${smId}`, body);
  }

  deleteSubmodel(systemType: AasSystemType, systemId: number, smId: string): Observable<any> {
    return this.http.delete(`${this.base(systemType, systemId)}/submodels/${smId}`);
  }

  createElement(systemType: AasSystemType, systemId: number, smId: string, body: any, parentPath?: string): Observable<any> {
    const url = `${this.base(systemType, systemId)}/submodels/${smId}/elements`;
    const params: any = {};
    if (parentPath) params.parentPath = parentPath;
    return this.http.post(url, body, { params });
  }

  putElement(systemType: AasSystemType, systemId: number, smId: string, path: string, body: any): Observable<any> {
    return this.http.put(`${this.base(systemType, systemId)}/submodels/${smId}/elements/${this.encodePathSegments(path)}`, body);
  }

  deleteElement(systemType: AasSystemType, systemId: number, smId: string, path: string): Observable<any> {
    return this.http.delete(`${this.base(systemType, systemId)}/submodels/${smId}/elements/${this.encodePathSegments(path)}`);
  }

  patchElementValue(systemType: AasSystemType, systemId: number, smId: string, path: string, body: any): Observable<any> {
    return this.http.patch(`${this.base(systemType, systemId)}/submodels/${smId}/elements/${this.encodePathSegments(path)}/value`, body);
  }

  uploadAasx(systemType: AasSystemType, systemId: number, file: File): Observable<any> {
    const url = `${this.base(systemType, systemId)}/upload`;
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post(url, form);
  }

  previewAasx(systemType: AasSystemType, systemId: number, file: File): Observable<any> {
    const url = `${this.base(systemType, systemId)}/upload/preview`;
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post(url, form);
    
  }

  attachSelectedAasx(systemType: AasSystemType, systemId: number, file: File, selection: any): Observable<any> {
    const url = `${this.base(systemType, systemId)}/upload/attach-selected`;
    const form = new FormData();
    form.append('file', file, file.name);
    form.append('selection', JSON.stringify(selection));
    return this.http.post(url, form);
  }

  private encodePathSegments(path: string): string {
    return path
      .split('/')
      .map(seg => encodeURIComponent(seg))
      .join('/');
  }
}
