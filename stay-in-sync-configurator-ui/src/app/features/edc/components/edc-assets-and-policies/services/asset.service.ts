import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Asset } from '../models/asset.model';
import { Observable, of } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AssetService {


  private mockAssets: Asset[] = [
    //{ id: 'asset-001', name: 'test', url: 'https://test.com/v1/images', type: 'HttpData', description: 'This is a description test', contentType: 'application/json' },

  ];

  constructor(private http: HttpClient) {}

  /**
   * Fetches the list of assets.
   * @returns A Promise that resolves with an array of Assets.
   */
  getAssets(): Promise<Asset[]> {
    // For now, we return the mock data wrapped in a resolved Promise.
    return Promise.resolve([...this.mockAssets]); // Return a copy
  }

  /**
   * An alternative way to fetch assets using Observables.
   * @returns An Observable that emits an array of Assets.
   */
  getAssetsObservable(): Observable<Asset[]> {
    // return this.http.get<Asset[]>('/api/assets');
    return of([...this.mockAssets]);
  }
}
