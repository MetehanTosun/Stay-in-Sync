import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { Asset } from '../models/asset.model';


@Injectable({
  providedIn: 'root'
})
export class AssetService {
  private baseUrl = 'http://localhost:8090/api/config/edcs';

  constructor(private http: HttpClient) {}

  getAssets(edcId: string): Observable<Asset[]> {
    return this.http.get<any[]>(`${this.baseUrl}/${edcId}/assets`).pipe(
      map((items) => {
        const mapped = items.map((raw) => this.toAsset(raw));
        // attach raw list for components that need full JSON
        (mapped as any).__rawList = items;
        return mapped;
      }),
      catchError(error => {
        console.error('Fehler beim Laden der Assets:', error);
        return throwError(() => error);
      })
    );
  }

  getAsset(edcId: string, assetId: string): Observable<Asset> {
    return this.http.get<any>(`${this.baseUrl}/${edcId}/assets/${assetId}`).pipe(
      map((raw) => this.toAsset(raw)),
      catchError(error => {
        console.error(`Fehler beim Laden des Assets ${assetId}:`, error);
        return throwError(() => error);
      })
    );
  }

  // Raw ODRL JSON for edit dialogs
  getAssetRaw(edcId: string, assetId: string): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/${edcId}/assets/${assetId}`).pipe(
      catchError(error => {
        console.error(`Fehler beim Laden des RAW Assets ${assetId}:`, error);
        return throwError(() => error);
      })
    );
  }

  createAsset(edcId: string, asset: Asset): Observable<Asset> {
    asset.targetEDCId = edcId;
    return this.http.post<Asset>(`${this.baseUrl}/${edcId}/assets`, asset).pipe(
      catchError(error => {
        console.error('Fehler beim Erstellen des Assets:', error);
        return throwError(() => error);
      })
    );
  }

  updateAsset(edcId: string, assetId: string, asset: Asset): Observable<Asset> {
    asset.id = assetId;
    asset.targetEDCId = edcId;
  return this.http.put<Asset>(`${this.baseUrl}/${edcId}/assets/${assetId}`, asset).pipe(
      catchError(error => {
        console.error(`Fehler beim Aktualisieren des Assets ${assetId}:`, error);
        return throwError(() => error);
      })
    );
  }

  deleteAsset(edcId: string, assetId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${edcId}/assets/${assetId}`).pipe(
      catchError(error => {
        console.error(`Fehler beim Löschen des Assets ${assetId}:`, error);
        return throwError(() => error);
      })
    );
  }

  // Returns static suggestions for query and header parameters used in the New Asset dialog
  getParamOptions(): Observable<{ query: { label: string; value: string }[]; header: { label: string; value: string }[] }> {
    const queryOptions = [
      { label: 'limit', value: 'limit' },
      { label: 'offset', value: 'offset' },
      { label: 'sort', value: 'sort' },
      { label: 'filter', value: 'filter' },
      { label: 'page', value: 'page' },
      { label: 'size', value: 'size' }
    ];

    const headerOptions = [
      { label: 'Authorization', value: 'Authorization' },
      { label: 'Content-Type', value: 'Content-Type' },
      { label: 'Accept', value: 'Accept' },
      { label: 'X-API-Key', value: 'X-API-Key' },
      { label: 'X-Request-ID', value: 'X-Request-ID' }
    ];

    return of({ query: queryOptions, header: headerOptions });
  }

  // Very light-weight endpoint autocompletion. Can be wired to a backend later if needed.
  getEndpointSuggestions(query: string): Observable<string[]> {
    const catalog: string[] = [
      'https://example.com/api',
      'https://example.com/api/resources',
      'https://httpbin.org/get',
      'https://httpbin.org/anything',
      'https://jsonplaceholder.typicode.com/posts',
      'https://jsonplaceholder.typicode.com/users',
      'https://api.github.com/repos',
    ];

    const q = (query || '').toLowerCase();
    const filtered = catalog.filter(u => u.toLowerCase().includes(q));
    return of(filtered.length > 0 ? filtered : catalog.slice(0, 5));
  }

  // Maps backend JSON (JSON-LD style) to our Asset interface used by the UI table
  private toAsset(raw: any): Asset {
    const propertiesArray = Array.isArray(raw?.properties)
      ? raw.properties
      : raw?.properties
        ? [raw.properties]
        : [];

    const firstProps = propertiesArray[0] || {};
    const contentType = raw?.contentType
      || firstProps['asset:prop:contenttype']
      || firstProps.contentType
      || '';

    const type = raw?.type
      || raw?.dataAddress?.type
      || '';

    const url = raw?.url
      || raw?.dataAddress?.baseUrl
      || raw?.dataAddress?.baseURL
      || raw?.dataAddress?.base_url
      || '';

    return {
      id: raw?.id, // not provided by backend (ignored), kept for compatibility
      assetId: raw?.assetId || raw?.['@id'] || '',
      name: raw?.name || firstProps['asset:prop:name'] || firstProps.name || '',
      description: raw?.description || firstProps['asset:prop:description'] || firstProps.description || '',
      contentType,
      type,
      url,
      targetEDCId: raw?.targetEDCId || raw?.target_edc_id || '',
      dataAddress: {
        id: raw?.dataAddress?.id,
        type: raw?.dataAddress?.type || type,
        base_url: raw?.dataAddress?.baseUrl || raw?.dataAddress?.baseURL || raw?.dataAddress?.base_url || url,
        proxyPath: raw?.dataAddress?.proxyPath ?? true,
        proxyQueryParams: raw?.dataAddress?.proxyQueryParams ?? true,
      },
      properties: propertiesArray.map((p: any) => ({
        id: p?.id,
        description: p?.description || p?.['asset:prop:description'] || '',
      })),
    } as Asset;
  }
 
}

