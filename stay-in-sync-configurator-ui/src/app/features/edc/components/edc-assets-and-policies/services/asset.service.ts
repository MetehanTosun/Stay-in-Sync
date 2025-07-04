import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { lastValueFrom, map } from 'rxjs';
import { Asset } from '../models/asset.model';

@Injectable({
  providedIn: 'root'
})
export class AssetService {
  private managementApiUrl = '/api/management/v2';


  private mockOdrlAssets: any[] = [
    {
      "@id": "asset-newsum-01",
      "properties": {
        "asset:prop:name": "Aggregierte Summe (Live)",
        "asset:prop:description": "Summe der Zahlen aus System A, B und C.",
        "asset:prop:contenttype": "application/json"
      },
      "dataAddress": {
        "type": "HttpData",
        "baseUrl": "http://mein-interner-datenservice:8080/current-sum"
      }
    },
    {
      "@id": "asset-weather-data-02",
      "properties": {
        "asset:prop:name": "Weather Data Feed",
        "asset:prop:description": "Live weather data for major cities.",
        "asset:prop:contenttype": "application/xml"
      },
      "dataAddress": {
        "type": "HttpData",
        "baseUrl": "http://weather-service/api/v1/data"
      }
    }
  ];

  constructor(private http: HttpClient) {}

  /**
   * Fetches all assets and transforms them from ODRL to the simple Asset model.
   */
  getAssets(): Promise<Asset[]> {

    const assets = this.mockOdrlAssets.map(odrlAsset => this.transformOdrlToAsset(odrlAsset));
    return Promise.resolve(assets);

    /*
    // REAL IMPLEMENTATION FOR BACKEND
    return lastValueFrom(
      this.http.get<any[]>(`${this.managementApiUrl}/assets`).pipe(
        map(odrlAssets => odrlAssets.map(this.transformOdrlToAsset))
      )
    );
    */
  }

  /**
   * Creates a new asset by building the ODRL structure from the ui.
   * @param asset The simple asset object from the UI form.
   */
  createAsset(asset: Asset): Promise<any> {
    const odrlAssetPayload = this.transformAssetToOdrl(asset);
    return this.uploadAsset(odrlAssetPayload);
  }

  /**
   * Uploads a pre-formatted ODRL asset JSON file.
   * This will be used by both manual creation and file upload.
   * @param odrlAsset The complete ODRL asset object.
   */
  uploadAsset(odrlAsset: any): Promise<any> {
    console.log("Posting to API:", JSON.stringify(odrlAsset, null, 2));

    // modifies the class property, making the change persistent.
    const index = this.mockOdrlAssets.findIndex(a => a['@id'] === odrlAsset['@id']);
    if (index !== -1) {
      this.mockOdrlAssets[index] = odrlAsset; // Update if exists
    } else {
      this.mockOdrlAssets.unshift(odrlAsset); // Add if new
    }

    return Promise.resolve({ message: 'Asset created/updated successfully!' });
    // return lastValueFrom(this.http.post(`${this.managementApiUrl}/assets`, odrlAsset));
  }

  /**
   * Updates an existing asset in the mock database.
   * In a real backend, this would be an HTTP PUT request.
   */
  updateAsset(assetToUpdate: Asset): Promise<void> {
    const index = this.mockOdrlAssets.findIndex(a => a['@id'] === assetToUpdate.id);
    if (index !== -1) {
      // Transform the UI model back to ODRL and update the mock "database".
      this.mockOdrlAssets[index] = this.transformAssetToOdrl(assetToUpdate);
      console.log('Mock Service: Updated asset', this.mockOdrlAssets[index]);
      return Promise.resolve();
    } else {
      console.error('Mock Service: Asset not found for update', assetToUpdate);
      return Promise.reject('Asset not found');
    }
  }

  /**
   * Deletes an asset from the mock database.
   * In a real backend, this would be an HTTP DELETE request.
   */
  deleteAsset(assetId: string): Promise<void> {
    const initialLength = this.mockOdrlAssets.length;
    this.mockOdrlAssets = this.mockOdrlAssets.filter(a => a['@id'] !== assetId);

    if (this.mockOdrlAssets.length < initialLength) {
      console.log('Mock Service: Deleted asset with id', assetId);
      return Promise.resolve();
    } else {
      console.error('Mock Service: Asset not found for deletion', assetId);
      return Promise.reject('Asset not found');
    }
  }

  //Helpers

  /**
   * Maps an ODRL to our Asset model for UI display.
   */
  private transformOdrlToAsset(odrlAsset: any): Asset {
    return {
      id: odrlAsset['@id'] || '',
      name: odrlAsset.properties?.['asset:prop:name'] || '',
      description: odrlAsset.properties?.['asset:prop:description'] || '',
      contentType: odrlAsset.properties?.['asset:prop:contenttype'] || '',
      type: odrlAsset.dataAddress?.type || '',
      url: odrlAsset.dataAddress?.baseUrl || ''
    };
  }

  /**
   * Builds the required ODRL JSON structure from our UI.
   */
  private transformAssetToOdrl(asset: Asset): any {
    // Use the existing ID for updates, or generate a new one for creations.
    const assetId = asset.id || 'asset-' + Math.random().toString(36).substring(2, 11);

    return {
      '@context': { "edc": "https://w3id.org/edc/v0.0.1/ns/" },
      '@id': assetId,
      'properties': {
        'asset:prop:name': asset.name,
        'asset:prop:description': asset.description,
        'asset:prop:contenttype': asset.contentType
      },
      'dataAddress': {
        'type': asset.type,
        'baseUrl': asset.url,
        'proxyPath': 'true'
      }
    };
  }
}
