import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Asset } from '../models/asset.model';

@Injectable({
  providedIn: 'root'
})
export class AssetService {

  private backendUrl = 'http://localhost:8090/api/config/edcs/assets'; 
  // 👆 anpassen falls dein Backend woanders läuft

  constructor(private http: HttpClient) {}

  /**
   * Alle Assets vom Backend laden
   */
  getAssets(): Observable<Asset[]> {
    return this.http.get<Asset[]>(this.backendUrl);
  }

  /**
   * Einzelnes Asset laden
   */
  getAsset(id: string): Observable<Asset> {
    return this.http.get<Asset>(`${this.backendUrl}/${id}`);
  }

  /**
   * Neues Asset anlegen
   */
  createAsset(asset: Asset): Observable<Asset> {
    return this.http.post<Asset>(this.backendUrl, asset);
  }

  /**
   * Bestehendes Asset aktualisieren
   */
  updateAsset(id: string, asset: Asset): Observable<Asset> {
    return this.http.put<Asset>(`${this.backendUrl}/${id}`, asset);
  }

  /**
   * Asset löschen
   */
  deleteAsset(id: string): Observable<void> {
    return this.http.delete<void>(`${this.backendUrl}/${id}`);
  }
}
