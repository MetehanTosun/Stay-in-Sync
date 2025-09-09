import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, delay, throwError } from 'rxjs';
import { catchError, tap, map, timeout } from 'rxjs/operators';
import { Asset } from '../models/asset.model';
import { MOCK_ODRL_ASSETS } from '../../../mocks/mock-data';

@Injectable({
  providedIn: 'root'
})
export class AssetService {
  // Mock-Modus - immer auf false lassen für echtes Backend
  private mockMode = false;

  // API-URLs
  private baseUrl = 'http://localhost:8090/api/config/edcs';
  private suggestionsUrl = 'http://localhost:8090/api/config/endpoint-suggestions';
  private paramOptionsUrl = 'http://localhost:8090/api/config/param-options';

  constructor(private http: HttpClient) {}

  /**
   * Lädt alle Assets im ODRL-Format (für Bearbeitung)
   */
  getOdrlAssets(edcId: string, cacheBreaker?: number): Observable<any[]> {
    if (this.mockMode) {
      console.warn(`Mock Mode: Fetching ODRL assets for EDC ID: ${edcId}`);
      return of(MOCK_ODRL_ASSETS[edcId] || []).pipe(delay(300));
    }
    
    // HTTP-Optionen mit verstärkter Cache-Kontrolle
    const params: any = {
      timestamp: (cacheBreaker || new Date().getTime()).toString(),
      nocache: 'true',
      _: new Date().getTime() // Zusätzlicher Cache-Brecher
    };
    
    // HTTP-Header zum Vermeiden von Cache
    const httpOptions = {
      params: params,
      headers: {
        'Cache-Control': 'no-cache, no-store, must-revalidate',
        'Pragma': 'no-cache',
        'Expires': '0'
      }
    };
    
    // Reale API-Anfrage mit Cache-Kontrolle
    const url = `${this.baseUrl}/assets/${edcId}/assets`;
    console.log(`Requesting ODRL assets with cache control from: ${url}`, httpOptions);
    
    return this.http.get<any[]>(url, httpOptions)
      .pipe(
        catchError((error: any) => {
          console.error('Error fetching assets for ODRL transformation:', error);
          console.error('Error status:', error.status);
          console.error('Error message:', error.message);
          
          // Bei bestimmten Fehlern wie 304 (Not Modified) könnten wir versuchen, einen anderen Ansatz zu verwenden
          if (error.status === 304) {
            console.log('Server returned 304 Not Modified, forcing fresh request...');
            // Versuche erneut mit anderen Cache-Brechern
            const newParams = { ...params, force: 'true', fresh: new Date().getTime() };
            return this.http.get<any[]>(url, { params: newParams, headers: httpOptions.headers });
          }
          
          throw error;
        }),
        tap((response: any) => {
          console.log('ODRL Assets response:', response);
          console.log('Response type:', Array.isArray(response) ? 'Array' : typeof response);
          console.log('Response length:', Array.isArray(response) ? response.length : 'Not an array');
        }),
        map((assets: any[]) => {
          if (!Array.isArray(assets)) {
            console.error('Unexpected response format for ODRL assets, expected array but got:', typeof assets);
            return [];
          }
          
          console.log(`Transforming ${assets.length} assets to ODRL format`);
          
          // Konvertiere die Backend-Assets in das ODRL-Format mit detaillierten Logs
          const odrlAssets = assets.map((asset, index) => {
            // Wir müssen sicherstellen, dass asset.assetId korrekt gesetzt ist
            const assetId = asset.assetId || (asset['@id'] ? asset['@id'] : (asset.id || `unknown-asset-${Date.now()}-${index}`));
            console.log(`Transforming asset ${index + 1}/${assets.length} with ID ${assetId} to ODRL`);
            
            // Erstelle eine ODRL-Darstellung des Assets
            const odrlAsset = {
              '@context': {
                'edc': 'https://w3id.org/edc/v0.0.1/ns/',
                'cx': 'https://w3id.org/cx/v0.1/ns/',
                'asset': 'https://w3id.org/asset/v0.1/ns/',
              },
              '@id': assetId,
              'properties': {
                'asset:prop:id': assetId,
                'asset:prop:name': asset.name || 'Unnamed Asset',
                'asset:prop:description': asset.description || '',
                'asset:prop:contenttype': asset.contentType || 'application/json',
                'asset:prop:version': asset.version || '1.0'
              },
              'dataAddress': {
                'type': asset.type || 'HttpData',
                'baseURL': asset.url || '',
                'proxyPath': true,
                'proxyQueryParams': true,
                'proxyBody': true,
                'proxyMethod': true,
                'filter': asset.filter || '',
                'contentType': asset.contentType || 'application/json',
                'headers': asset.headers || {}
              }
            };
            
            console.log(`ODRL asset created:`, odrlAsset);
            return odrlAsset;
          });
          
          console.log(`ODRL transformation complete. Total assets: ${odrlAssets.length}`);
          return odrlAssets;
        })
      );
  }

  /**
   * Lädt alle Assets für eine EDC-Instanz
   */
  getAssets(edcId: string, cacheBreaker?: number): Observable<Asset[]> {
    if (this.mockMode) {
      console.warn(`Mock Mode: Fetching assets for EDC ID: ${edcId}`);
      const odrlAssets = MOCK_ODRL_ASSETS[edcId] || [];
      const mappedAssets = odrlAssets.map((asset: any) => ({
        assetId: asset['@id'],
        name: asset.properties['asset:prop:name'],
        description: asset.properties['asset:prop:description'],
        contentType: asset.properties['asset:prop:contenttype'],
        type: asset.dataAddress.type,
        ...asset,
      }));
      return of(mappedAssets as Asset[]).pipe(delay(300));
    }
    
    // HTTP-Optionen mit verstärkter Cache-Kontrolle
    const params: any = {
      timestamp: (cacheBreaker || new Date().getTime()).toString(),
      nocache: 'true',
      _: new Date().getTime() // Zusätzlicher Cache-Brecher
    };
    
    // HTTP-Header zum Vermeiden von Cache
    const httpOptions = {
      params: params,
      headers: {
        'Cache-Control': 'no-cache, no-store, must-revalidate',
        'Pragma': 'no-cache',
        'Expires': '0'
      }
    };
    
    // Reale API-Anfrage mit Cache-Kontrolle
    const url = `${this.baseUrl}/assets/${edcId}/assets`;
    console.log(`Requesting assets with cache control from: ${url}`, httpOptions);
    
    return this.http.get<any[]>(url, httpOptions)
      .pipe(
        catchError((error: any) => {
          console.error('Error fetching assets:', error);
          console.error('Error status:', error.status);
          console.error('Error message:', error.message);
          
          // Bei bestimmten Fehlern wie 304 (Not Modified) könnten wir versuchen, einen anderen Ansatz zu verwenden
          if (error.status === 304) {
            console.log('Server returned 304 Not Modified, forcing fresh request...');
            // Versuche erneut mit anderen Cache-Brechern
            const newParams = { ...params, force: 'true', fresh: new Date().getTime() };
            return this.http.get<any[]>(url, { params: newParams, headers: httpOptions.headers });
          }
          
          throw error;
        }),
        tap((response: any) => {
          console.log('Assets response:', response);
          console.log('Response type:', Array.isArray(response) ? 'Array' : typeof response);
          console.log('Response length:', Array.isArray(response) ? response.length : 'Not an array');
        }),
        map((response: any[]) => {
          if (!Array.isArray(response)) {
            console.error('Unexpected response format, expected array but got:', typeof response);
            return [];
          }
          
          // Ergebnis ausführlich loggen
          console.log(`Received ${response.length} assets from server`);
          
          // Detaillierte Protokollierung jedes Assets
          const mappedAssets = response.map((item, index) => {
            console.log(`Processing asset ${index + 1}/${response.length}:`, item);
            return this.mapBackendResponseToAsset(item);
          });
          
          console.log('Final mapped assets count:', mappedAssets.length);
          return mappedAssets;
        })
      );
  }

  /**
   * Lädt ein einzelnes Asset
   */
  getAsset(edcId: string, assetId: string): Observable<Asset> {
    if (this.mockMode) {
      console.warn(`Mock Mode: Fetching asset ${assetId} for EDC ID: ${edcId}`);
      const asset = (MOCK_ODRL_ASSETS[edcId] || []).find((a: any) => a['@id'] === assetId);
      return of(asset as Asset).pipe(delay(300));
    }
    
    console.log(`Requesting asset from: ${this.baseUrl}/assets/${edcId}/assets/${encodeURIComponent(assetId)}`);
    return this.http.get<Asset>(`${this.baseUrl}/assets/${edcId}/assets/${encodeURIComponent(assetId)}`)
      .pipe(
        catchError((error: any) => {
          console.error(`Error fetching asset ${assetId}:`, error);
          throw error;
        }),
        tap((response: any) => console.log('Asset response:', response))
      );
  }

  /**
   * Erstellt ein neues Asset
   */
  createAsset(edcId: string, asset: any): Observable<any> {
    if (this.mockMode) {
      console.warn(`Mock Mode: Creating asset for EDC ID: ${edcId}`);
      const newAsset = { ...asset, '@id': asset['@id'] || `asset-${Date.now()}` };
      if (!MOCK_ODRL_ASSETS[edcId]) {
        MOCK_ODRL_ASSETS[edcId] = [];
      }
      MOCK_ODRL_ASSETS[edcId].push(newAsset);
      return of(newAsset).pipe(delay(300));
    }
    
    // Sicherstellen, dass targetEDCId gesetzt ist
    asset.targetEDCId = edcId;
    
    // Stellen sicher, dass keine leeren Felder im Asset sind
    this.validateAndFixAsset(asset);
    
    // Vollständigen Request loggen
    console.log(`Creating asset at: ${this.baseUrl}/assets/${edcId}/assets`);
    console.log('Asset payload:', JSON.stringify(asset, null, 2));

    return this.http.post<any>(`${this.baseUrl}/assets/${edcId}/assets`, asset)
      .pipe(
        catchError((error: any) => {
          console.error(`Error creating asset for EDC ${edcId}:`, error);
          if (error.error && error.error.details) {
            console.error('Server error details:', error.error.details);
          }
          if (error.error && error.error.message) {
            console.error('Server error message:', error.error.message);
          }
          throw error;
        }),
        tap((response: any) => {
          console.log('Create asset response:', response);
          // Zusätzliche Validierung der Antwort
          if (!response || (typeof response === 'object' && Object.keys(response).length === 0)) {
            console.warn('Server returned empty or invalid response');
          }
        })
      );
  }

  /**
   * Aktualisiert ein bestehendes Asset
   */
  /**
   * Updates an existing asset using the ID for URL path and the full ODRL asset object as payload
   */
  updateAsset(edcId: string, assetId: string, assetJson: any): Observable<any> {
    if (this.mockMode) {
      return this.createAsset(edcId, assetJson) as Observable<any>;
    }
    
    console.log(`Updating asset at: ${this.baseUrl}/assets/${edcId}/assets/${encodeURIComponent(assetId)}`);
    
    // Stelle sicher, dass das Asset die richtige ID und EDC-ID hat
    assetJson.targetEDCId = edcId;
    
    // Stelle sicher, dass dataAddress korrekt formatiert ist
    if (assetJson.dataAddress) {
      // Konvertiere base_url zu base_url für die API
      if (assetJson.dataAddress.baseURL || assetJson.dataAddress.baseUrl) {
        assetJson.dataAddress.base_url = assetJson.dataAddress.baseURL || assetJson.dataAddress.baseUrl;
        delete assetJson.dataAddress.baseURL;
        delete assetJson.dataAddress.baseUrl;
      }
    }
    
    console.log('Asset Payload:', JSON.stringify(assetJson, null, 2));
    
    return this.http.put<any>(`${this.baseUrl}/assets/${edcId}/assets/${encodeURIComponent(assetId)}`, assetJson)
      .pipe(
        catchError((error: any) => {
          console.error(`Error updating asset ${assetId}:`, error);
          console.error('Error details:', error.error);
          throw error;
        }),
        tap((response: any) => console.log('Update asset response:', response))
      );
  }

  /**
   * Holt Endpunkt-Vorschläge für den Asset-Dialog
   */
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

  /**
   * Holt Parameter-Optionen für den Asset-Dialog
   */
  getParamOptions(): Observable<{ query: any[], header: any[] }> {
    if (this.mockMode) {
      console.warn('Mock Mode: Fetching parameter options.');
      const mockOptions = {
        query: [{ label: 'Limit', value: 'limit' }, { label: 'Offset', value: 'offset' }],
        header: [{ label: 'X-API-Key', value: 'X-API-Key' }, { label: 'Authorization', value: 'Authorization' }]
      };
      return of(mockOptions).pipe(delay(100));
    }
    
    return this.http.get<{ query: any[], header: any[] }>(this.paramOptionsUrl);
  }

  /**
   * Löscht ein Asset
   */
  deleteAsset(edcId: string, assetId: string): Observable<any> {
    if (this.mockMode) {
      const mockResponse = { success: true, message: 'Asset deleted (mock)' };
      return of(mockResponse);
    }
    
    // Korrigierte URL basierend auf der API-Dokumentation
    const url = `${this.baseUrl}/assets/${edcId}/assets/${encodeURIComponent(assetId)}`;
    console.log(`Deleting asset at: ${url}`);
    
    // Hier verwenden wir timeout, um einen Timeout von 10 Sekunden zu setzen
    return this.http.delete<any>(url)
      .pipe(
        timeout(10000), // 10 Sekunden Timeout
        catchError((error: any) => {
          console.error(`Error deleting asset ${assetId}:`, error);
          if (error.name === 'TimeoutError') {
            console.error('Delete request timed out');
            throw new Error('Delete operation timed out. Please try again.');
          }
          
          if (error.status === 404) {
            console.warn(`Asset ${assetId} not found, considering it as deleted`);
            return of({ success: true, message: 'Asset not found or already deleted' });
          }
          
          console.error('Error details:', error.error);
          throw error;
        }),
        tap((response: any) => console.log('Delete asset response:', response))
      );
  }  /**
   * Mappt die Backend-Antwort auf das Asset-Modell
   */
  private mapBackendResponseToAsset(item: any): Asset {
    // Sicherstellen, dass wir eine gültige assetId haben, indem wir verschiedene mögliche Quellen prüfen
    const assetId = item.assetId || (item['@id'] ? item['@id'] : (item.id ? item.id : `unknown-asset-${Date.now()}`));
    
    console.log('Mapping backend asset to UI model:', item);
    
    return {
      id: item.id || undefined,
      assetId: assetId,
      name: item.properties?.['asset:prop:name'] || item.name || assetId || 'Unnamed Asset',
      url: item.url || item.dataAddress?.baseURL || '',
      type: item.type || item.dataAddress?.type || '',
      contentType: item.contentType || item.properties?.['asset:prop:contenttype'] || '',
      description: item.description || item.properties?.['asset:prop:description'] || '',
      targetEDCId: item.targetEDCId || '',
      dataAddress: {
        jsonLDType: item.dataAddress?.jsonLDType || 'DataAddress',
        type: item.dataAddress?.type || 'HttpData',
        base_url: item.dataAddress?.base_url || item.dataAddress?.baseURL || '',
        proxyPath: item.dataAddress?.proxyPath || true,
        proxyQueryParams: item.dataAddress?.proxyQueryParams || true
      },
      properties: item.properties || { description: '' }
    };
  }
  
  /**
   * Validiert und korrigiert ein Asset, stellt sicher, dass keine leeren Pflichtfelder vorhanden sind
   */
  private validateAndFixAsset(asset: any): void {
    // Stellen sicher, dass properties vorhanden sind
    if (!asset.properties) {
      asset.properties = {};
    }
    
    // Grundlegende Pflichtfelder überprüfen und einfügen
    if (!asset.properties['asset:prop:name'] || asset.properties['asset:prop:name'].trim() === '') {
      asset.properties['asset:prop:name'] = asset['@id'] || `Asset-${Date.now()}`;
    }
    
    // Stelle sicher, dass die Beschreibung nicht leer ist
    if (!asset.properties['asset:prop:description'] || asset.properties['asset:prop:description'].trim() === '') {
      asset.properties['asset:prop:description'] = `Beschreibung für ${asset['@id'] || 'Asset'}`;
    }
    
    if (!asset.properties['asset:prop:contenttype']) {
      asset.properties['asset:prop:contenttype'] = 'application/json';
    }
    
    // DataAddress überprüfen und korrigieren
    if (!asset.dataAddress) {
      asset.dataAddress = { type: 'HttpData' };
    }
    
    if (!asset.dataAddress.type) {
      asset.dataAddress.type = 'HttpData';
    }
    
    // Korrigiere base_url - Feld muss genau so im Backend ankommen
    if (asset.dataAddress.baseUrl || asset.dataAddress.baseURL) {
      asset.dataAddress.base_url = asset.dataAddress.baseUrl || asset.dataAddress.baseURL;
      delete asset.dataAddress.baseUrl;
      delete asset.dataAddress.baseURL;
    }
    
    if (!asset.dataAddress.base_url || asset.dataAddress.base_url.trim() === '') {
      asset.dataAddress.base_url = 'https://example.com/api/' + (asset['@id'] || `asset-${Date.now()}`);
    }
    
    // Proxy-Einstellungen hinzufügen, falls nicht vorhanden
    if (asset.dataAddress.proxyPath === undefined) {
      asset.dataAddress.proxyPath = true;
    }
    
    if (asset.dataAddress.proxyQueryParams === undefined) {
      asset.dataAddress.proxyQueryParams = true;
    }
  }
}
