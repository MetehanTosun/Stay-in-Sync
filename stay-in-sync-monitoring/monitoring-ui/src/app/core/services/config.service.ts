import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

/**
 * ConfigService is responsible for retrieving configuration values
 * from the backend API.
 *
 * Currently, it provides methods to fetch the base URLs for Grafana
 * and SyncNode, which can be used by other services or components.
 */
@Injectable({ providedIn: 'root' })
export class ConfigService {
  constructor(private readonly http: HttpClient) {}

  /**
   * Retrieves the Grafana base URL from the backend API.
   *
   * @returns A promise that resolves to the Grafana base URL as a string.
   * @throws If the request fails, the promise will reject with an HTTP error.
   */
  async getGrafanaBaseUrl(): Promise<string> {
    return firstValueFrom(
      this.http.get('/api/config/grafanaUrl', { responseType: 'text' })
    );
  }

  /**
   * Retrieves the SyncNode base URL from the backend API.
   *
   * @returns A promise that resolves to the SyncNode base URL as a string.
   * @throws If the request fails, the promise will reject with an HTTP error.
   */
  async getCoreManagementUrl(): Promise<string> {
    return firstValueFrom(
      this.http.get('/api/config/coreManagementUrl', { responseType: 'text' })
    );
  }
}
