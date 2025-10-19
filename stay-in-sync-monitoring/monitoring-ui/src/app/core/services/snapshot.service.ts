import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {SnapshotDTO} from '../models/snapshot.model';
import {Observable} from 'rxjs';

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
    return this.http.get<SnapshotDTO>(`${this.baseUrl}/latest`, {
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
    return this.http.get<SnapshotDTO[]>(`${this.baseUrl}/list`, {
      params: { transformationId },
    });
  }

  /**
  * Retrieves a snapshot by its ID.
  *
  * @param id The ID of the snapshot.
   * @returns Observable that emits the corresponding SnapshotDTO object.
  */
  getById(id: string): Observable<SnapshotDTO> {
    return this.http.get<SnapshotDTO>(`${this.baseUrl}/${id}`);
  }

}
