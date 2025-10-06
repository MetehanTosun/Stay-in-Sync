import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { TransformationModelForSnapshotPanel } from '../models/transformation.model';

/**
 * TransformationService
 *
 * Provides methods to fetch transformation data from the backend API.
 */
@Injectable({
  providedIn: 'root'
})
export class TransformationService {
  /** Base URL for transformation API endpoints */
  private readonly baseUrl = '/api/transformation';

  constructor(private readonly http: HttpClient) {}

  /**
   * Fetch transformations, optionally filtered by sync job ID.
   *
   * @param syncJobId (Optional) ID of the synchronization job to filter transformations.
   * @returns Observable emitting an array of TransformationModelForSnapshotPanel.
   */
  getTransformations(syncJobId?: string) {
    return this.http.get<TransformationModelForSnapshotPanel[]>(`${this.baseUrl}/${syncJobId}`);
  }
}
