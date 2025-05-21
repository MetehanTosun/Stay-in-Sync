import { Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {LogEntry} from '../../node.model';

@Injectable({ providedIn: 'root' })
export class LogService {
  getLogs(): Observable<LogEntry[]> {

    const mockLogs: LogEntry[] = [
      { timestamp: '2024-06-01T12:00:00Z', message: 'Node A gestartet', nodeId: 'A' },
      { timestamp: '2024-06-01T12:01:00Z', message: 'Node B verbunden', nodeId: 'B' },
      { timestamp: '2024-06-01T12:02:00Z', message: 'Node C Fehler', nodeId: 'C' },
      { timestamp: '2024-06-01T12:03:00Z', message: 'System läuft stabil' }
    ];
    return new Observable<LogEntry[]>(observer => {
      observer.next(mockLogs);
      observer.complete();
    });
  }
}



