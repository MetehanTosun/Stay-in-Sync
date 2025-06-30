import { Injectable } from '@angular/core';
import {Observable} from 'rxjs/internal/Observable';
import {HttpClient} from '@angular/common/http';
import {SyncJob} from '../models/sync-job.model';

@Injectable({
  providedIn: 'root'
})
export class SyncJobService {

  constructor(readonly httpClient: HttpClient) { }


  getAll(): Observable<SyncJob[]> {
    return this.httpClient.get<SyncJob[]>(`/api/config/sync-job`);
  }

  delete(syncJob: SyncJob): Observable<void> {
    return this.httpClient.delete<void>(`/api/config/sync-job/${syncJob.id}`);
  }

  create(syncJob: SyncJob): Observable<SyncJob> {
    return this.httpClient.post<SyncJob>(`/api/config/sync-job`, syncJob);
  }

  getById(selectedSyncJobId: number) {
    return this.httpClient.get<SyncJob>(`/api/config/sync-job/${selectedSyncJobId}`);
  }

  update(selectedSyncJobId: number,syncJob: SyncJob): Observable<SyncJob> {
    return this.httpClient.put<SyncJob>(`/api/config/sync-job/${selectedSyncJobId}`, syncJob);
  }
}
