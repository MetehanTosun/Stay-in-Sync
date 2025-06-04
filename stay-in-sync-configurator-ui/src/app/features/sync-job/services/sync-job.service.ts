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
}
