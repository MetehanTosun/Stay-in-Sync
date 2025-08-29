import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, delay } from 'rxjs';
import { Asset } from '../models/asset.model';
import { MOCK_ODRL_ASSETS } from '../../../mocks/mock-data';

@Injectable({
  providedIn: 'root'
})
export class AssetService {


  // UI Testing method. To use the real backend, change this to false!
  private mockMode = false;


  private backendUrl = 'http://localhost:8090/api/config/edcs/assets';
  // 👆 anpassen falls dein Backend woanders läuft

  private baseUrl = 'http://localhost:8090/api/config/edcs';

  constructor(private http: HttpClient) {}

  /**
   * Alle Assets vom Backend laden
   */
  // getAssets(): Observable<Asset[]> {
  //   return this.http.get<Asset[]>(this.backendUrl);

  getAssets(edcId: string): Observable<Asset[]> {

    if (this.mockMode) {
      console.warn(`Mock Mode: Fetching assets for EDC ID: ${edcId}`);
      const odrlAssets = MOCK_ODRL_ASSETS[edcId] || [];
      // The component expects a mapped object for the table view.
      const mappedAssets = odrlAssets.map((asset: any) => ({
        assetId: asset['@id'],
        name: asset.properties['asset:prop:name'],
        description: asset.properties['asset:prop:description'],
        contentType: asset.properties['asset:prop:contenttype'],
        type: asset.dataAddress.type,
        ...asset, // Pass the full object for the details view
      }));


      return of(mappedAssets as Asset[]).pipe(delay(300));
    }
    return this.http.get<Asset[]>(`${this.baseUrl}/${edcId}/assets`);
  }

  /**
   * Einzelnes Asset laden
   */
  // getAsset(id: string): Observable<Asset> {
  //   return this.http.get<Asset>(`${this.backendUrl}/${id}`);

  getAsset(edcId: string, assetId: string): Observable<Asset> {
    if (this.mockMode) {
      console.warn(`Mock Mode: Fetching asset ${assetId} for EDC ID: ${edcId}`);
      const asset = (MOCK_ODRL_ASSETS[edcId] || []).find((a: any) => a['@id'] === assetId);
      return of(asset as Asset).pipe(delay(300));
    }
    return this.http.get<Asset>(`${this.baseUrl}/${edcId}/assets/${assetId}`);
  }

  /**
   * Neues Asset anlegen
   */
  // createAsset(asset: Asset): Observable<Asset> {
  //   return this.http.post<Asset>(this.backendUrl, asset);

  createAsset(edcId: string, asset: any): Observable<any> {
    if (this.mockMode) {
      console.warn(`Mock Mode: Creating/updating asset for EDC ID: ${edcId}`);
      if (!MOCK_ODRL_ASSETS[edcId]) {
        MOCK_ODRL_ASSETS[edcId] = [];
      }
      const existingIndex = MOCK_ODRL_ASSETS[edcId].findIndex(
        (a: any) => a['@id'] === asset['@id']
      );
      if (existingIndex > -1) {
        MOCK_ODRL_ASSETS[edcId][existingIndex] = asset; // Update
      } else {
        MOCK_ODRL_ASSETS[edcId].push(asset); // Create
      }
      return of(asset).pipe(delay(300));
    }
    return this.http.post<any>(`${this.baseUrl}/${edcId}/assets`, asset);
  }

  /**
   * Bestehendes Asset aktualisieren
   */
  // updateAsset(id: string, asset: Asset): Observable<Asset> {
  //   return this.http.put<Asset>(`${this.backendUrl}/${id}`, asset);

  updateAsset(edcId: string, assetId: string, asset: Asset): Observable<Asset> {
    if (this.mockMode) {
      // The createAsset mock logic handles updates (upsert)
      return this.createAsset(edcId, asset) as Observable<Asset>;
    }
    return this.http.put<Asset>(`${this.baseUrl}/${edcId}/assets/${assetId}`, asset);
  }

  /**
   * Asset löschen
   */
  // deleteAsset(id: string): Observable<void> {
  //   return this.http.delete<void>(`${this.backendUrl}/${id}`);

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
    return this.http.delete<void>(`${this.baseUrl}/${edcId}/assets/${assetId}`);
  }
}
