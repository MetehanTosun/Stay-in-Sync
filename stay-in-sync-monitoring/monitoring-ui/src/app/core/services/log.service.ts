import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';
import { LogEntry } from '../models/log.model';

@Injectable({ providedIn: 'root' })
export class LogService {
  private baseUrl = '/api/logs';

  constructor(private http: HttpClient) {}

  /**
   * Logs für eine Liste von TransformationIds abrufen
   */
  getLogsByTransformations(
    transformationIds: string[],
    startTime: number,
    endTime: number,
    level: string
  ): Observable<LogEntry[]> {
    let params = new HttpParams()
      .set('startTime', startTime)
      .set('endTime', endTime)
      .set('level', level);

    // POST mit Body = TransformationIds
    return this.http.post<LogEntry[]>(`${this.baseUrl}/transformations`, transformationIds, { params });
  }

  /**
   * Alle Logs ohne Filter abrufen (optional, z.B. für globale Suche)
   */
  getLogs(
    startTime: number,
    endTime: number,
    level: string
  ): Observable<LogEntry[]> {
    let params = new HttpParams()
      .set('startTime', startTime)
      .set('endTime', endTime)
      .set('level', level);

    return this.http.get<LogEntry[]>(this.baseUrl, { params });
  }

}
