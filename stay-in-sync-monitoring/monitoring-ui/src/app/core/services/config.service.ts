// src/app/core/services/config.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ConfigService {
  constructor(private http: HttpClient) {}

  async getGrafanaBaseUrl(): Promise<string> {
    return firstValueFrom(
      this.http.get('/api/config/grafanaUrl', { responseType: 'text' })
    );
  }
}
