import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/internal/Observable';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Transformation, UpdateTransformationRequest} from '../models/transformation.model';
import {JobDeploymentStatus} from '../../../shared/components/job-status-tag/job-status-tag.component';
import {TransformationStatusUpdate} from '../models/transformation-status.model';

@Injectable({
  providedIn: 'root'
})
export class TransformationService {

  constructor(readonly httpClient: HttpClient) {
  }

  getAll(): Observable<Transformation[]> {
    return this.httpClient.get<Transformation[]>(`/api/config/transformation`);
  }

  getAllWithoutSyncJob(): Observable<Transformation[]> {
    const params = new HttpParams().set('withSyncJob', 'false');
    return this.httpClient.get<Transformation[]>(`/api/config/transformation`, {params});
  }

  create(transformation: Transformation): Observable<Transformation> {
    return this.httpClient.post<Transformation>(`/api/config/transformation`, transformation);
  }

  delete(transformation: Transformation): Observable<void> {
    return this.httpClient.delete<void>(`/api/config/transformation/${transformation.id}`);
  }

  update(transformation: UpdateTransformationRequest): Observable<Transformation> {
    return this.httpClient.put<Transformation>(`/api/config/transformation/${transformation.id}`, transformation);
  }


  getById(id: number): Observable<Transformation> {
    return this.httpClient.get<Transformation>(`/api/config/transformation/${id}`);
  }

  getBySyncJobId(id: number): Observable<Transformation[]> {
    const params = new HttpParams().set('syncJobId', id);
    return this.httpClient.get<Transformation[]>(`/api/config/transformation`, {params});
  }

  manageDeployment(id: number, deploymentStatus: JobDeploymentStatus): Observable<void> {
    const headers = {'Content-Type': 'application/json'};
    return this.httpClient.put<void>(`/api/config/transformation/${id}/deployment`, `"${deploymentStatus}"`, {headers});
  }

  addRule(id: number, ruleId: number): Observable<void> {
    return this.httpClient.put<void>(`/api/config/transformation/${id}/rule/${ruleId}`, {});
  }

  removeRule(id: number): Observable<void> {
    return this.httpClient.delete<void>(`/api/config/transformation/${id}/rule`);
  }

  watchDeploymentStatus(syncJobId: number): Observable<TransformationStatusUpdate> {
    console.log("watching status " +syncJobId)
    return new Observable<TransformationStatusUpdate>(observer => {
      const eventSource = new EventSource(`/api/config/transformation/status?syncJobId=${syncJobId}`);

      eventSource.onmessage = (event) => {
        const update = JSON.parse(event.data);
        observer.next(update);
      };

      eventSource.onerror = (error) => {
        console.error("SSE error:", error);
        observer.error(error);
      };

      return () => {
        console.log("Closing SSE connection");
        eventSource.close();
      };
    });
  }

}
