import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { SnapshotModel } from '../models/snapshot.model';

/**
 * SnapshotService
 *
 * Provides methods to fetch snapshot data related to transformations from the backend API.
 */
@Injectable({
  providedIn: 'root',
})
export class SnapshotService {
  /** Base URL for snapshot API endpoints */
  private readonly baseUrl = '/api/snapshots';

  constructor(private readonly http: HttpClient) {}

  /**
   * Fetch the latest snapshot for a given transformation.
   *
   * @param transformationId ID of the transformation.
   * @returns Observable emitting the latest SnapshotModel.
   */
  getLatestSnapshot(transformationId: string) {
    return this.http.get<SnapshotModel>(`${this.baseUrl}/latest`, {
      params: { transformationId },
    });
  }

  /**
   * Fetch the last five snapshots for a given transformation.
   *
   * @param transformationId ID of the transformation.
   * @returns Observable emitting an array of SnapshotModel.
   */
  getLastFiveSnapshots(transformationId: string) {
    return this.http.get<SnapshotModel[]>(`${this.baseUrl}/list`, {
      params: { transformationId },
    });
  }
}
