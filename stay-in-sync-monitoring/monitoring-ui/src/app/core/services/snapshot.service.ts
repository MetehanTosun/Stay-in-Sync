import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { SnapshotModel } from '../models/snapshot.model';

/**
 * SnapshotService
 *
 * Provides methods to fetch snapshot data for transformations
 * from the monitoring backend.
 */
@Injectable({
  providedIn: 'root',
})
export class SnapshotService {
  /**
   * Base URL of the monitoring backend API.
   * TODO: Replace hardcoded URL with environment configuration.
   */
  private baseUrl = 'http://localhost:8091';

  constructor(private http: HttpClient) {}

  /**
   * Fetch the latest snapshot for a given transformation.
   *
   * @param transformationId The transformation ID as a string.
   * @returns An Observable emitting the latest snapshot.
   */
  getLatestSnapshot(transformationId: string) {
    const url = `${this.baseUrl}/monitoring/snapshots/latest`;
    return this.http.get<SnapshotModel>(url, {
      params: { transformationId },
    });
  }

  /**
   * Fetch the last five snapshots for a given transformation.
   *
   * @param transformationId The transformation ID as a string.
   * @returns An Observable emitting an array of up to five snapshots.
   */
  getLastFiveSnapshots(transformationId: string) {
    const url = `${this.baseUrl}/monitoring/snapshots/list`;
    return this.http.get<SnapshotModel[]>(url, {
      params: { transformationId },
    });
  }
}
