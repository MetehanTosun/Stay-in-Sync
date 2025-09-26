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
    level?: string
  ): Observable<LogEntry[]> {
    let params = new HttpParams()
      .set('startTime', startTime)
      .set('endTime', endTime);

    if (level) {
      params = params.set('level', level);
    }

    return this.http.post<LogEntry[]>(`${this.baseUrl}/transformations`, transformationIds, { params });
  }


  /**
   * Alle Logs ohne Filter abrufen
   */
  getLogs(
    startTime: number,
    endTime: number,
    level?: string
  ): Observable<LogEntry[]> {
    let params = new HttpParams()
      .set('startTime', startTime)
      .set('endTime', endTime);

    if (level) {
      params = params.set('level', level);
    }

    return this.http.get<LogEntry[]>(this.baseUrl, { params });
  }

  getLogsByService(
    service: string,
    startTime: number,
    endTime: number,
    level?: string
  ): Observable<LogEntry[]> {
    let params = new HttpParams()
      .set('service', service)
      .set('startTime', startTime)
      .set('endTime', endTime);

    if (level) {
      params = params.set('level', level);
    }

    return this.http.get<LogEntry[]>(`${this.baseUrl}/service`, { params });
  }

}

