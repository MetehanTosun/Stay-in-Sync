import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { catchError, map, delay, tap } from 'rxjs/operators';
import { Asset } from '../models/asset.model';
import { Transformation } from '../../../models/transformation.model';
import { MOCK_ODRL_ASSETS, MOCK_TRANSFORMATIONS, MOCK_TARGET_ARC_CONFIGS } from '../../../mocks/mock-data';
import { TargetSystemDTO } from '../../../../target-system/models/targetSystemDTO';

@Injectable({
  providedIn: 'root'
})
export class AssetService {
  // UI Testing method. To use the real backend, change this to false!
  private mockMode = false;
  
private backendApiUrl = 'http://localhost:8090/api/config'; 
private baseUrl = `${this.backendApiUrl}/edcs`;
private transformationsUrl = `${this.backendApiUrl}/transformations`;
private targetArcUrl = `${this.backendApiUrl}/target-arc`;
private targetSystemsUrl = `${this.backendApiUrl}/target-systems`;

  constructor(private http: HttpClient) {}

  getAssets(edcId: string): Observable<Asset[]> {
    if (this.mockMode) {
      console.warn(`Mock Mode: Fetching assets for EDC ID: ${edcId}`);
      const rawAssets = MOCK_ODRL_ASSETS[edcId] || [];
      const mapped = rawAssets.map((raw) => this.toAsset(raw));
      (mapped as any).__rawList = rawAssets; // Keep the raw list for components that need full JSON
      return of(mapped).pipe(delay(300));
    }
    
    // Verwende Cache-Busting durch Hinzufügen eines Zeitstempels zur URL
    const timestamp = new Date().getTime();
    
    console.log(`Fetching assets for EDC ID ${edcId} with cache-busting timestamp ${timestamp}`);
    
    return this.http.get<any[]>(`${this.baseUrl}/${edcId}/assets?_t=${timestamp}`, {
      headers: { 
        'Cache-Control': 'no-cache, no-store, must-revalidate', 
        'Pragma': 'no-cache', 
        'Expires': '0' 
      }
    }).pipe(
      tap(items => console.log(`Fetched ${items?.length || 0} assets from backend for EDC ID ${edcId}`)),
      map((items) => {
        if (!items) {
          console.warn('Received null or undefined items from backend');
          return [];
        }
        
        console.log('Raw assets:', items);
        const mapped = items.map((raw) => this.toAsset(raw));
        console.log('Mapped assets:', mapped);
        
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

    // Mache eine Kopie vom Asset, damit wir es anpassen können ohne das Original zu verändern
    const assetCopy = { ...asset };

    // Wichtig: Richtige Format-Anpassung für das Backend
    const properties: Record<string, any> = {
      'asset:prop:name': asset.name,
      'asset:prop:description': asset.description || '',
      'asset:prop:contenttype': asset.contentType || 'application/json',
      'asset:prop:version': '1.0.0'
    };
    
    // Wenn ein Target-System-ID existiert, füge sie hinzu
    if ('targetSystemId' in asset && asset.targetSystemId) {
      properties['asset:prop:targetSystemId'] = asset.targetSystemId;
    }

    // Wenn eine Transformation-ID existiert, füge sie hinzu
    // Wir nutzen bracket-notation, da transformationId nicht im Interface ist
    if ('transformationId' in asset && asset['transformationId']) {
      properties['asset:prop:transformationId'] = asset['transformationId'];
    }
    
    // Prüfe auch im properties-Array, falls vorhanden
    if (asset.properties && Array.isArray(asset.properties)) {
      asset.properties.forEach(prop => {
        if (prop.id === 'asset:prop:transformationId' && prop.description) {
          properties['asset:prop:transformationId'] = prop.description;
        }
        if (prop.id === 'asset:prop:targetSystemId' && prop.description) {
          properties['asset:prop:targetSystemId'] = prop.description;
        }
      });
    }
    
    const formattedAsset = {
      '@id': assetId,
      '@type': 'Asset',
      targetEDCId: parseInt(edcId),
      properties,
      dataAddress: {
        '@type': 'DataAddress',
        type: asset.dataAddress?.type || 'HttpData',
        baseUrl: (asset.dataAddress as any)?.baseUrl || '',
        proxyPath: asset.dataAddress?.proxyPath !== false,
        proxyQueryParams: asset.dataAddress?.proxyQueryParams !== false
      }
    };

    console.log('Sending formatted asset for update:', formattedAsset);
    
    return this.http.put<Asset>(`${this.baseUrl}/${edcId}/assets/${assetId}`, formattedAsset, {
      headers: { 
        'Content-Type': 'application/json',
        'Cache-Control': 'no-cache, no-store, must-revalidate'
      }
    }).pipe(
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
    
    console.log(`Sending delete request to: ${this.baseUrl}/${edcId}/assets/${assetId}`);
    console.log(`EDC ID: ${edcId} (${typeof edcId}), Asset ID: ${assetId} (${typeof assetId})`);
    
    if (!assetId || assetId === 'undefined') {
      console.error('Asset ID is undefined or empty!');
      return throwError(() => new Error('Asset ID is required for deletion'));
    }
    
    // Einfacherer Ansatz: Wir machen eine normale DELETE-Anfrage ohne observe: 'response'
    return this.http.delete<any>(
      `${this.baseUrl}/${edcId}/assets/${assetId}`, 
      { 
        headers: { 
          'Cache-Control': 'no-cache, no-store, must-revalidate', 
          'Pragma': 'no-cache', 
          'Expires': '0' 
        },
        responseType: 'text' as 'json' // Wichtig: Wir erwarten einen Text als Antwort vom Server
      }
    ).pipe(
      tap(response => {
        console.log(`Asset ${assetId} successfully deleted from backend with response:`, response);
      }),
      map(() => {
        // Geben wir void zurück wie erwartet
        return;
      }),
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
  // Methode zum Abrufen der verfügbaren Target Systems
  getTargetSystems(): Observable<TargetSystemDTO[]> {
    if (this.mockMode) {
      console.warn('Mock Mode: Fetching target systems');
      return of(MOCK_TRANSFORMATIONS.map(t => ({
        id: Number(t.id),
        name: t.alias,
        apiUrl: '',
        apiType: 'REST_OPENAPI'
      }))).pipe(delay(300));
    }
    
    return this.http.get<TargetSystemDTO[]>(this.targetSystemsUrl).pipe(
      catchError(error => {
        console.error('Fehler beim Laden der Target Systems:', error);
        return throwError(() => error);
      })
    );
  }

  private toAsset(raw: any): Asset {
    // The 'properties' can be an object or an array with one object. Normalize to a single object.
    const props = Array.isArray(raw?.properties) ? (raw.properties[0] || {}) : (raw?.properties || {});
    const dataAddress = raw?.dataAddress || {};
    
    console.log('Raw asset from backend:', raw);
    console.log('Asset ID (numeric):', raw?.id);
    console.log('Asset @id:', raw?.['@id']);

    // Verwende die originale ID aus dem Backend (falls vorhanden) und die @id als assetId
    const backendId = raw?.id || null;
    const assetId = raw?.['@id'] || '';
    
    // Extrahiere targetSystemId und transformationId aus den Properties, falls vorhanden
    const targetSystemId = props['asset:prop:targetSystemId'] || null;
    const transformationId = props['asset:prop:transformationId'] || null;
    
    // Erstelle das Asset-Objekt
    const asset: Asset & Record<string, any> = {
      id: backendId, // Numerische ID aus dem Backend
      assetId: assetId, // Die EDC-Asset-ID (@id)
      name: props['asset:prop:name'] || raw?.['@id'] || '', // Fallback to assetId if name is missing
      description: props['asset:prop:description'] || '',
      contentType: props['asset:prop:contenttype'] || 'application/json',
      type: dataAddress?.type || 'HttpData',
      url: dataAddress?.baseUrl || dataAddress?.base_url || '',
      targetEDCId: raw?.targetEDCId || raw?.target_edc_id || '',
      targetSystemId: targetSystemId,

      dataAddress: dataAddress,
      queryParams: dataAddress?.queryParams, // Keep for easy access in details view
      headers: {},

      properties: Object.keys(props).map(key => ({
        id: key,
        description: props[key]
      })),
    };
    
    // Füge transformationId hinzu, wenn vorhanden
    if (transformationId !== null) {
      asset['transformationId'] = transformationId;
    }
    
    return asset as Asset;
  }
}
