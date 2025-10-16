import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { catchError, map, delay } from 'rxjs/operators';
import { Asset } from '../models/asset.model';
import { Transformation } from '../../../models/transformation.model';
import { MOCK_ODRL_ASSETS, MOCK_TRANSFORMATIONS, MOCK_TARGET_ARC_CONFIGS } from '../../../mocks/mock-data';

@Injectable({
  providedIn: 'root'
})
export class AssetService {
  // UI Testing method. To use the real backend, change this to false!
  private mockMode = true;

  private baseUrl = 'http://localhost:8090/api/config/edcs';
  private transformationsUrl = 'http://localhost:8090/api/config/transformations';
  private targetArcUrl = 'http://localhost:8090/api/config/target-arc';

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

      const { assetId, targetEDCId, ...rest } = asset;
      const cleanAsset = { ...rest, '@id': asset.assetId };

      (MOCK_ODRL_ASSETS[edcId] || []).push(cleanAsset);
      return of(this.toAsset(cleanAsset)).pipe(delay(300));
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

        const { assetId, targetEDCId, ...rest } = asset;
        const cleanAsset = { ...rest, '@id': asset.assetId };
        (MOCK_ODRL_ASSETS[edcId] || [])[index] = cleanAsset;
        return of(this.toAsset(cleanAsset)).pipe(delay(300));
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
        return throwError(() => new Error(`Failed to delete asset ${assetId}`));
      })
    );
  }

  /**
   * Fetches the list of available Transformations
   */
  getTransformations(): Observable<Transformation[]> {
    if (this.mockMode) {
      console.warn('Mock Mode: Fetching transformations.');
      return of(MOCK_TRANSFORMATIONS).pipe(delay(100));
    }
    return this.http.get<Transformation[]>(this.transformationsUrl).pipe(
      catchError(err => {
        console.error('Failed to fetch transformations', err);
        return throwError(() => new Error('Could not load transformations from backend.'));
      })
    );
  }


  /**
   * Fetches the detailed configuration for a specific Target Arc.
   * @param id The ID of the target arc.
   */
  getTargetArcConfig(id: string): Observable<any> {
    if (this.mockMode) {
      console.warn(`Mock Mode: Fetching config for Target Arc ID: ${id}`);
      const config = MOCK_TARGET_ARC_CONFIGS[id] || { dataAddress: {} };
      return of(config).pipe(delay(200));
    }
    return this.http.get<any>(`${this.targetArcUrl}/${id}`).pipe(
      catchError(err => {
        console.error(`Failed to fetch config for target arc ${id}`, err);
        return throwError(() => new Error(`Could not load configuration for the selected system.`));
      })
    );
  }

  getEndpointSuggestions(query: string): Observable<string[]> {

    if (this.mockMode) {
      const suggestions = ['https://example.com/api/v1', 'https://example.com/api/v2'];
      return of(suggestions.filter(s => s.includes(query)));
    }
    // Replace with a real backend call if needed
    return this.http.get<string[]>(this.baseUrl + '/endpoint-suggestions', { params: { q: query } });
  }

  // Maps backend JSON (JSON-LD style) to our Asset interface used by the UI table
  private toAsset(raw: any): Asset {
    // The 'properties' can be an object or an array with one object. Normalize to a single object.
    const props = Array.isArray(raw?.properties) ? (raw.properties[0] || {}) : (raw?.properties || {});
    const dataAddress = raw?.dataAddress || {};

    return {
      id: raw?.id, // not provided by backend (ignored), kept for compatibility
      assetId: raw?.['@id'] || '', // The primary ID is at the top level
      name: props['asset:prop:name'] || raw?.['@id'] || '', // Fallback to assetId if name is missing
      description: props['asset:prop:description'] || '',
      contentType: props['asset:prop:contenttype'] || 'application/json',
      type: dataAddress?.type || 'HttpData',
      url: dataAddress?.baseUrl || dataAddress?.base_url || '',
      targetEDCId: raw?.targetEDCId || raw?.target_edc_id || '',


      dataAddress: dataAddress,
      queryParams: dataAddress?.queryParams, // Keep for easy access in details view
      headers: {},

      properties: Object.keys(props).map(key => ({
        id: key,
        description: props[key]
      })),
    } as Asset;
  }
}
