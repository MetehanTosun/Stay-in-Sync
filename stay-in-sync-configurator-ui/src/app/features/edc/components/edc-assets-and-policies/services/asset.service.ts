import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { lastValueFrom, map } from 'rxjs';
import { Asset } from '../models/asset.model';

@Injectable({
  providedIn: 'root'
})
export class AssetService {
  private managementApiUrl = '/api/management/v2';

  constructor(private http: HttpClient) {}

  /**
   * Fetches all assets and transforms them from ODRL to the simple Asset model.
   */
  getAssets(): Promise<Asset[]> {
    const url = `${this.managementApiUrl}/assets`;

    // Example of an API call returning ODRL data.
    const mockOdrlApiResponse = [
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

    // transformOdrlToAsset function will map and insert the info to our model.
    const assets = mockOdrlApiResponse.map(this.transformOdrlToAsset);
    return Promise.resolve(assets);

    /*
    // REAL IMPLEMENTATION FOR BACKEND
    return lastValueFrom(
      this.http.get<any[]>(url).pipe(
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
    const url = `${this.managementApiUrl}/assets`;
    console.log("Posting to API:", JSON.stringify(odrlAsset, null, 2));
    // return lastValueFrom(this.http.post(url, odrlAsset));
    return Promise.resolve({ message: 'Asset created successfully!' }); // Mocking success
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
    // Generate a unique ID if one isn't provided - TO CHANGE LATER
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
