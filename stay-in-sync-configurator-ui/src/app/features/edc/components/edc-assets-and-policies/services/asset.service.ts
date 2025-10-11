import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError, of, delay } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { Asset } from '../models/asset.model';
import { TargetSystem } from '../../../models/target-system.model';


@Injectable({
  providedIn: 'root'
})
export class AssetService {

  private baseUrl = 'http://localhost:8090/api/config/edcs';
  private suggestionsUrl = 'http://localhost:8090/api/config/endpoint-suggestions';
  private paramOptionsUrl = 'http://localhost:8090/api/config/param-options';
  private targetSystemConfigUrl = 'http://localhost:8090/api/config/target-system-configurations';

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

  redeployAsset(edcId: string, assetId: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/${edcId}/assets/${assetId}/redeploy`, {});
  }

  /**
   * Fetches asset templates for the asset creation dialog.
   */
  getAssetTemplates(): Observable<{ name: string; content: any }[]> {
    return this.http.get<{ name: string; content: any }[]>(`${this.baseUrl}/asset-templates`);
  }

  /**
   * Fetches the list of available Target Systems (ID and alias).
   */
  getTargetSystems(): Observable<TargetSystem[]> {
    return this.http.get<TargetSystem[]>(this.targetSystemConfigUrl);
  }

  /**
   * Fetches the detailed configuration for a specific Target System.
   * @param targetSystemId The ID of the target system.
   */
  getTargetSystemConfig(targetSystemId: string): Observable<any> {
    return this.http.get<any>(`${this.targetSystemConfigUrl}/${targetSystemId}`);
  }


  // Returns static suggestions for query and header parameters used in the New Asset dialog
  getParamOptions(): Observable<{ query: { label: string; value: string }[]; header: { label: string; value: string }[] }> {

    return this.http.get<{ query: any[], header: any[] }>(this.paramOptionsUrl);
  }


  getEndpointSuggestions(query: string): Observable<string[]> {
    return this.http.get<string[]>(this.suggestionsUrl, { params: { q: query } });
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
    
    const type = raw?.dataAddress?.type || 'HttpData';
    const url = raw?.dataAddress?.base_url
      || raw?.dataAddress?.baseUrl
      || raw?.dataAddress?.baseURL
      || '';

    const result = {
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
        type: type,
        base_url: url,
        proxyPath: raw?.dataAddress?.proxyPath ?? true,
        proxyQueryParams: raw?.dataAddress?.proxyQueryParams ?? true,
      },
      properties: propertiesArray.map((p: any) => ({
        id: p?.id,
        description: p?.description || p?.['asset:prop:description'] || '',
      })),
      thirdPartyChanges: raw?.thirdPartyChanges,
    };
    return result as Asset;
  }

}
