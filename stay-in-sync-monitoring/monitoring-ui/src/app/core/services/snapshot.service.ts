import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SnapshotDTO } from '../models/snapshot.model';

/**
 * SnapshotService
 * ----------------
 * Provides methods for retrieving snapshot data associated with transformations
 * from the backend API.
 *
 * A snapshot represents a recorded state of a transformation at a specific moment
 * (e.g., after an execution or during an error). This service allows components
 * to load individual snapshots, the latest snapshot, or recent history for display
 * and debugging in the Replay UI.
 *
 * Responsibilities:
 * - Retrieve the latest snapshot for a given transformation via {@link getLatestSnapshot}.
 * - Fetch the last five recent snapshots for a transformation via {@link getLastFiveSnapshots}.
 * - Retrieve a specific snapshot by its ID using {@link getById}.
 *
 * @see SnapshotDTO for the data structure of snapshots.
 *
 * @author Mohammed-Ammar Hassnou
 */
@Injectable({
  providedIn: 'root',
})
export class SnapshotService {
  /** Base URL for snapshot API endpoints */
  private readonly baseUrl = '/api/snapshots';

  constructor(private readonly http: HttpClient) {}

  /**
   * Retrieve the latest snapshot for a given transformation.
   *
   * Sends an HTTP GET request to `/api/snapshots/latest` with the transformation ID
   * as a query parameter and returns the most recent snapshot.
   *
   * @param transformationId The unique ID of the transformation whose latest snapshot should be fetched.
   * @returns Observable emitting the latest {@link SnapshotDTO} instance.
   */
  getLatestSnapshot(transformationId: string) {
    return this.http.get<SnapshotDTO>(`${this.baseUrl}/latest`, {
      params: { transformationId },
    });
  }

  /**
   * Retrieve up to five most recent snapshots for a given transformation.
   *
   * Sends an HTTP GET request to `/api/snapshots/list` with the transformation ID
   * as a query parameter and returns a list of snapshot DTOs ordered by creation time.
   *
   * @param transformationId The unique ID of the transformation whose recent snapshots should be fetched.
   * @returns Observable emitting an array of {@link SnapshotDTO} objects.
   */
  getLastFiveSnapshots(transformationId: string) {
    return this.http.get<SnapshotDTO[]>(`${this.baseUrl}/list`, {
      params: { transformationId },
    });
  }

  /**
   * Retrieve a snapshot by its unique identifier.
   *
   * Sends an HTTP GET request to `/api/snapshots/{id}` and returns the corresponding
   * {@link SnapshotDTO} if it exists.
   *
   * @param id The unique identifier of the snapshot.
   * @returns Observable emitting the requested {@link SnapshotDTO}.
   */
  getById(id: string): Observable<SnapshotDTO> {
    return this.http.get<SnapshotDTO>(`${this.baseUrl}/${id}`);
  }
}
