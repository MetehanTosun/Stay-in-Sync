import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AasService {
  constructor(private http: HttpClient) {}

  aasTest(sourceSystemId: number): Observable<any> {
    const url = `/api/config/source-system/${sourceSystemId}/aas/test`;
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
    const url = `/api/config/source-system/${sourceSystemId}/aas/submodels/${submodelId}/elements`;
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
    valuePayload: any
  ): Observable<any> {
    const url = `/api/config/source-system/${sourceSystemId}/aas/submodels/${submodelId}/elements/${elementPath}/value`;
    return this.http.patch(url, valuePayload);
  }

  encodeIdToBase64Url(id: string): string {
    if (!id) return id;
    const enc = typeof window !== 'undefined' && (window as any).btoa ? (window as any).btoa : (str: string) => Buffer.from(str, 'utf-8').toString('base64');
    return enc(id)
      .replace(/=+$/g, '')
      .replace(/\+/g, '-')
      .replace(/\//g, '_');
  }
}


