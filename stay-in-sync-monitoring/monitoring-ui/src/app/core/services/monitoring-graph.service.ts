import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, timeout } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class MonitoringGraphService {
  constructor(private http: HttpClient) {}

  getMonitoringGraphData(): Observable<any> {
    return this.http.get<any>('/api/monitoringgraph').pipe(
      timeout(10000), // brich nach 10s im Frontend ab
      catchError(err => {
        console.error('Fehler beim Laden des Monitoring-Graphen:', err);
        return throwError(() => new Error('Monitoring-Graph konnte nicht geladen werden.'));
      })
    );
  }
}
