import { Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {LogEntry} from '../models/log.model';
import {HttpClient} from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class LogService {

  private lokiApiUrl = 'http://localhost:3100';

  constructor(private http: HttpClient) {}

  getFilteredLogs(nodeId: string, startTime: string, endTime: string, level: string): Observable<LogEntry[]> {
    const params = {
      query: `{nodeId="${nodeId}",level="${level}"}`,
      start: startTime,
      end: endTime,
    };
    console.log(`Fetching logs for Node: ${nodeId}, Start: ${startTime}, End: ${endTime}, Level: ${level}`);
    return this.http.get<LogEntry[]>(this.lokiApiUrl, { params });
  }
}



