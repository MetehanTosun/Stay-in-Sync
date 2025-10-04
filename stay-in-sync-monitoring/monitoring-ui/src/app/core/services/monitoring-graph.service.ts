import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, timeout } from 'rxjs/operators';

/**
 * MonitoringGraphService
 *
 * Provides methods to fetch monitoring graph data from the backend API.
 * The service applies a frontend timeout and proper error handling.
 */
@Injectable({ providedIn: 'root' })
export class MonitoringGraphService {
  constructor(private readonly http: HttpClient) {}

  /**
   * Fetches monitoring graph data from the backend.
   *
   * The request is automatically aborted if it takes longer than 10 seconds.
   * Any error during the request is caught and transformed into a user-friendly error.
   *
   * @returns Observable emitting the monitoring graph data.
   * @throws Observable error if the request fails or times out.
   */
  getMonitoringGraphData(): Observable<any> {
    return this.http.get<any>('/api/monitoringgraph').pipe(
      timeout(10000), // Abort request after 10s
      catchError(err => {
        console.error('Error fetching monitoring graph data:', err);
        return throwError(() => new Error('Monitoring graph could not be loaded.'));
      })
    );
  }
}
