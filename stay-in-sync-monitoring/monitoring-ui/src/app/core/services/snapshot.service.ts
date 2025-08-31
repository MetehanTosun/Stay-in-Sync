import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { SnapshotModel } from '../models/snapshot.model';
@Injectable({
  providedIn: 'root',
})
export class SnapshotService {
  constructor(private http: HttpClient) {}

  private baseUrl = 'http://localhost:8091';

  getLatestSnapshot(transformationId: string) {
    const url = `${this.baseUrl}/monitoring/snapshots/latest`;
    return this.http.get<SnapshotModel>(url, {
      params: { transformationId },
    });
  }

  getLastFiveSnapshots(transformationId: string) {
    const url = `${this.baseUrl}/monitoring/snapshots/list`;
    return this.http.get<SnapshotModel[]>(url, {
      params: { transformationId },
    });
  }
}
