import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { SnapshotModel } from '../models/snapshot.model';

@Injectable({
  providedIn: 'root',
})
export class SnapshotService {
  private baseUrl = '/api/snapshots';

  constructor(private http: HttpClient) {}

  getLatestSnapshot(transformationId: string) {
    return this.http.get<SnapshotModel>(`${this.baseUrl}/latest`, {
      params: { transformationId },
    });
  }

  getLastFiveSnapshots(transformationId: string) {
    return this.http.get<SnapshotModel[]>(`${this.baseUrl}/list`, {
      params: { transformationId },
    });
  }
}
