import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError, of, delay } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { Asset } from '../models/asset.model';
import { MOCK_ODRL_ASSETS } from '../../../mocks/mock-data';
import { TargetSystem } from '../../../models/target-system.model';


@Injectable({
  providedIn: 'root'
})
export class AssetService {


  private mockMode = false;

  private baseUrl = 'http://localhost:8090/api/config/edcs';
  private suggestionsUrl = 'http://localhost:8090/api/config/endpoint-suggestions';
  private paramOptionsUrl = 'http://localhost:8090/api/config/param-options';
  private targetSystemConfigUrl = 'http://localhost:8090/api/config/target-system-configurations';

  constructor(private http: HttpClient) {}

  getAssets(edcId: string): Observable<Asset[]> {
    if (this.mockMode) {
      console.warn(`Mock Mode: Fetching assets for EDC ID: ${edcId}`);
      const rawAssets = MOCK_ODRL_ASSETS[edcId] || [];
      const mapped = rawAssets.map((raw) => this.toAsset(raw));
      (mapped as any).__rawList = rawAssets; // Keep the raw list for components that need full JSON
      return of(mapped).pipe(delay(300));
    }
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
    if (this.mockMode) {
      console.warn(`Mock Mode: Fetching asset ${assetId}.`);
      const rawAsset = (MOCK_ODRL_ASSETS[edcId] || []).find(a => a['@id'] === assetId);
      return of(this.toAsset(rawAsset));
    }
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
    if (this.mockMode) {
      console.warn(`Mock Mode: Fetching raw asset ${assetId}.`);
      return of((MOCK_ODRL_ASSETS[edcId] || []).find(a => a['@id'] === assetId));
    }
    return this.http.get<any>(`${this.baseUrl}/${edcId}/assets/${assetId}`).pipe(
      catchError(error => {
        console.error(`Fehler beim Laden des RAW Assets ${assetId}:`, error);
        return throwError(() => error);
      })
    );
  }

  createAsset(edcId: string, asset: Asset): Observable<Asset> {
    if (this.mockMode) {
      console.warn('Mock Mode: Creating asset.');
      const newAsset = { ...asset, '@id': asset.assetId, syncStatus: 'SYNCED' };
      (MOCK_ODRL_ASSETS[edcId] || []).push(newAsset);
      return of(this.toAsset(newAsset)).pipe(delay(300));
    }
    asset.targetEDCId = edcId;
    return this.http.post<Asset>(`${this.baseUrl}/${edcId}/assets`, asset).pipe(
      catchError(error => {
        console.error('Fehler beim Erstellen des Assets:', error);
        return throwError(() => error);
      })
    );
  }

  updateAsset(edcId: string, assetId: string, asset: Asset): Observable<Asset> {
    if (this.mockMode) {
      console.warn(`Mock Mode: Updating asset ${assetId}.`);
      const index = (MOCK_ODRL_ASSETS[edcId] || []).findIndex(a => a['@id'] === assetId);
      if (index > -1) {
        const updatedAsset = { ...asset, '@id': asset.assetId, syncStatus: 'SYNCED' };
        (MOCK_ODRL_ASSETS[edcId] || [])[index] = updatedAsset;
        return of(this.toAsset(updatedAsset)).pipe(delay(300));
      }
      return of(asset);
    }
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
    if (this.mockMode) {
      console.warn(`Mock Mode: Deleting asset ${assetId}.`);
      const index = (MOCK_ODRL_ASSETS[edcId] || []).findIndex(a => a['@id'] === assetId);
      if (index > -1) {
        (MOCK_ODRL_ASSETS[edcId] || []).splice(index, 1);
      }
      return of(undefined).pipe(delay(300));
    }
    return this.http.delete<void>(`${this.baseUrl}/${edcId}/assets/${assetId}`).pipe(
      catchError(error => {
        console.error(`Fehler beim Löschen des Assets ${assetId}:`, error);
        return throwError(() => error);
      })
    );
  }

  redeployAsset(edcId: string, assetId: string): Observable<void> {
    if (this.mockMode) {
      console.warn(`Mock Mode: Redeploying asset ${assetId}.`);
      const asset = (MOCK_ODRL_ASSETS[edcId] || []).find(a => a['@id'] === assetId);
      if (asset) asset.thirdPartyChanges = false;
      return of(undefined).pipe(delay(300));
    }

    return this.http.post<void>(`${this.baseUrl}/${edcId}/assets/${assetId}/redeploy`, {});
  }

  /**
   * Fetches asset templates for the asset creation dialog.
   */
  getAssetTemplates(): Observable<{ name: string; content: any }[]> {
    if (this.mockMode) {
      console.warn('Mock Mode: Fetching asset templates.');
      const mockTemplates = [
        {
          name: 'Standard HTTP Data Asset',
          content: {
            "@context": { "edc": "https://w3id.org/edc/v0.0.1/ns/" },
            "@id": "",
            "properties": {
              "asset:prop:name": "",
              "asset:prop:description": "",
              "asset:prop:contenttype": "application/json",
              "asset:prop:version": "1.0.0"
            },
            "dataAddress": {
              "type": "HttpData",
              "base_url": ""
            }
          }
        }
      ];
      return of(mockTemplates).pipe(delay(100));
    }
    return this.http.get<{ name: string; content: any }[]>(`${this.baseUrl}/asset-templates`);
  }

  /**
   * Fetches the list of available Target Systems (ID and alias).
   */
  getTargetSystems(): Observable<TargetSystem[]> {
    if (this.mockMode) {
      console.warn('Mock Mode: Fetching target systems.');
      const mockSystems: TargetSystem[] = [
        { id: '1', alias: 'User Management API' },
        { id: '2', alias: 'Events API' }
      ];
      return of(mockSystems).pipe(delay(100));
    }
    return this.http.get<TargetSystem[]>(this.targetSystemConfigUrl);
  }

  /**
   * Fetches the detailed configuration for a specific Target System.
   * @param targetSystemId The ID of the target system.
   */
  getTargetSystemConfig(targetSystemId: string): Observable<any> {
    if (this.mockMode) {
      console.warn(`Mock Mode: Fetching config for Target System ID: ${targetSystemId}`);

      const mockConfig = {
        "1": {
          "dataAddress": {
            "baseUrl": "https://api.example.com/users",
            "proxyQueryParams": "true",
            "header:Authorization": "Bearer <YOUR_USER_API_TOKEN>"
          }
        },
        "2": {
          "dataAddress": {
            "baseUrl": "https://api.example.com/events",
            "queryParams": "activeOnly=true",
            "header:Accept": "application/json"
          }
        }
      };
      const config = (mockConfig as any)[targetSystemId] || { dataAddress: {} };
      return of(config).pipe(delay(200));
    }
    return this.http.get<any>(`${this.targetSystemConfigUrl}/${targetSystemId}`);
  }


  // Returns static suggestions for query and header parameters used in the New Asset dialog
  getParamOptions(): Observable<{ query: { label: string; value: string }[]; header: { label: string; value: string }[] }> {

    return this.http.get<{ query: any[], header: any[] }>(this.paramOptionsUrl);
  }


  getEndpointSuggestions(query: string): Observable<string[]> {
    if (this.mockMode) {
      console.warn('Mock Mode: Fetching endpoint suggestions.');
      const allEndpoints = [
        'http://localhost:8080/api/data',
        'https://example.com/data/v1',
        'https://my-backend.com/service'
      ];
      const filteredEndpoints = allEndpoints.filter(e => e.toLowerCase().includes(query.toLowerCase()));
      return of(filteredEndpoints).pipe(delay(100));
    }
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

    const type = raw?.type
      || raw?.dataAddress?.type
      || '';

    const url = raw?.url
      || raw?.dataAddress?.baseUrl
      || raw?.dataAddress?.baseURL
      || raw?.dataAddress?.base_url
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
        type: raw?.dataAddress?.type || type,
        base_url: raw?.dataAddress?.baseUrl || raw?.dataAddress?.baseURL || raw?.dataAddress?.base_url || url,
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
