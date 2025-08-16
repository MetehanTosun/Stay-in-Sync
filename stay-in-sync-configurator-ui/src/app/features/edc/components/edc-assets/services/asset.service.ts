// src/app/features/edc/services/asset.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Asset } from '../models/asset.model';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AssetService {
  private backendUrl = '/api/config/edcs/assets';

  constructor(private http: HttpClient) {}

  getAssets(): Observable<Asset[]> {
    return this.http.get<Asset[]>(this.backendUrl);
  }

  createAsset(asset: Asset): Observable<Asset> {
    return this.http.post<Asset>(this.backendUrl, asset);
  }

  deleteAsset(assetId: string): Observable<void> {
    return this.http.delete<void>(`${this.backendUrl}/${assetId}`);
  }
}
