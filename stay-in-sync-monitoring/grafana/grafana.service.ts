import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class GrafanaService {
  private grafanaUrl = 'http://localhost:3000'; // ggf. adapt
  private apiKey = ''; //API KEY- Sicherheit ?

  constructor(private http: HttpClient) {}

  getSnapshots(): Observable<any> {
    const headers = new HttpHeaders({
      'Authorization': this.apiKey
    });
    return this.http.get(`${this.grafanaUrl}/api/snapshots`, { headers });
  }
}
