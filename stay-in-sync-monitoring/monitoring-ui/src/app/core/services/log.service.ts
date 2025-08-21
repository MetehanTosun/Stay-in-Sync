import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';
import {LogEntry} from '../models/log.model';

@Injectable({ providedIn: 'root' })
export class LogService {
  private baseUrl = '/logs';

  constructor(private http: HttpClient) {}

  /**
   * Logs holen (optional mit syncJobId)
   */
  getLogs(
    startTime: number,
    endTime: number,
    level: string = 'info',
    syncJobId?: string
  ): Observable<LogEntry[]> {
    let params = new HttpParams()
      .set('startTime', startTime)
      .set('endTime', endTime)
      .set('level', level);

    if (syncJobId) {
      // eigener Endpoint: /api/logs/SyncJob/{id}
      return this.http.get<LogEntry[]>(`${this.baseUrl}/SyncJob/${syncJobId}`, { params });
    } else {
      // Standard-Endpoint: /api/logs
      return this.http.get<LogEntry[]>(this.baseUrl, { params });
    }
  }

  // getLogs(
  //   stream: 'stdout' | 'stderr',
  //   level: string,
  //   nodeId: string,
  //   startTime: string,
  //   endTime: string
  // ): Observable<any[]> {
  //   // Labels
  //   let labelParts = [`stream="${stream}"`];
  //   if (nodeId) labelParts.push(`nodeId="${nodeId}"`);
  //   const labelSelector = `{${labelParts.join(',')}}`;
  //
  //   // Log level
  //   const query = level ? `${labelSelector} |= "level=${level}"` : labelSelector;
  //
  //   const params = new HttpParams()
  //     .set('query', query)
  //     .set('start', startTime)
  //     .set('end', endTime)
  //     .set('limit', '1000')
  //     .set('direction', 'backward');
  //
  //   console.log('Loki Query:', query);
  //
  //   return this.http.get(this.baseUrl, { params, responseType: 'text' }).pipe(
  //     map(raw => {
  //       const response = JSON.parse(raw);
  //       if (!response.data || !response.data.result) return [];
  //       return response.data.result.flatMap((stream: any) =>
  //         stream.values.map((entry: [string, string]) => {
  //           const parsed = JSON.parse(entry[1]);
  //           return {
  //             timestamp: parsed.time,
  //             message: parsed.log
  //           };
  //         })
  //       );
  //     })
  //   );
  // }


}
