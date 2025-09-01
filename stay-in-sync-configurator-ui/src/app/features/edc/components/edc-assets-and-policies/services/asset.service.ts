import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, delay } from 'rxjs';
import { catchError, tap, map } from 'rxjs/operators';
import { Asset } from '../models/asset.model';
import { MOCK_ODRL_ASSETS } from '../../../mocks/mock-data';

@Injectable({
  providedIn: 'root'
})
export class AssetService {
  // Mock-Modus - immer auf false lassen für echtes Backend
  private mockMode = false;

  // API-URLs
  private baseUrl = 'http://localhost:8090/api/config/edcs/assets';
  private suggestionsUrl = 'http://localhost:8090/api/config/endpoint-suggestions';
  private paramOptionsUrl = 'http://localhost:8090/api/config/param-options';

  constructor(private http: HttpClient) {}

  /**
   * Lädt alle Assets für eine EDC-Instanz
   */
  getAssets(edcId: string): Observable<Asset[]> {
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
    
    // Reale API-Anfrage
    console.log(`Requesting assets from: ${this.baseUrl}/${edcId}/assets`);
    return this.http.get<any[]>(`${this.baseUrl}/${edcId}/assets`)
      .pipe(
        catchError((error: any) => {
          console.error('Error fetching assets:', error);
          throw error;
        }),
        tap((response: any) => console.log('Assets response:', response)),
        map((response: any[]) => {
          if (!Array.isArray(response)) {
            console.error('Unexpected response format, expected array but got:', typeof response);
            return [];
          }
          
          return response.map(item => this.mapBackendResponseToAsset(item));
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
    
    console.log(`Requesting asset from: ${this.baseUrl}/${edcId}/assets/${assetId}`);
    return this.http.get<Asset>(`${this.baseUrl}/${edcId}/assets/${assetId}`)
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
    console.log(`Creating asset at: ${this.baseUrl}/${edcId}/assets`);
    console.log('Asset payload:', JSON.stringify(asset, null, 2));
    
    return this.http.post<any>(`${this.baseUrl}/${edcId}/assets`, asset)
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
  updateAsset(edcId: string, assetId: string, asset: Asset): Observable<Asset> {
    if (this.mockMode) {
      return this.createAsset(edcId, asset) as Observable<Asset>;
    }
    
    console.log(`Updating asset at: ${this.baseUrl}/${edcId}/assets/${assetId}`);
    return this.http.put<Asset>(`${this.baseUrl}/${edcId}/assets/${assetId}`, asset)
      .pipe(
        catchError((error: any) => {
          console.error(`Error updating asset ${assetId}:`, error);
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
  deleteAsset(edcId: string, assetId: string): Observable<void> {
    if (this.mockMode) {
      console.warn(`Mock Mode: Deleting asset ${assetId} for EDC ID: ${edcId}`);
      if (MOCK_ODRL_ASSETS[edcId]) {
        MOCK_ODRL_ASSETS[edcId] = MOCK_ODRL_ASSETS[edcId].filter(
          (a: any) => a['@id'] !== assetId
        );
      }
      return of(undefined).pipe(delay(300));
    }
    
    console.log(`Deleting asset at: ${this.baseUrl}/${edcId}/assets/${assetId}`);
    return this.http.delete<void>(`${this.baseUrl}/${edcId}/assets/${assetId}`)
      .pipe(
        catchError((error: any) => {
          console.error(`Error deleting asset ${assetId}:`, error);
          throw error;
        }),
        tap(() => console.log(`Asset ${assetId} deleted successfully`))
      );
  }

  /**
   * Mappt die Backend-Antwort auf das Asset-Modell
   */
  private mapBackendResponseToAsset(item: any): Asset {
    return {
      id: item.id || undefined,
      assetId: item['@id'] || '',
      name: item.properties?.['asset:prop:name'] || item['@id'] || 'Unnamed Asset',
      url: item.url || '',
      type: item.type || '',
      contentType: item.contentType || '',
      description: item.description || item.properties?.['asset:prop:description'] || '',
      targetEDCId: item.targetEDCId || '',
      dataAddress: {
        jsonLDType: item.dataAddress?.jsonLDType || 'DataAddress',
        type: item.dataAddress?.type || 'HttpData',
        base_url: item.dataAddress?.base_url || '',
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
