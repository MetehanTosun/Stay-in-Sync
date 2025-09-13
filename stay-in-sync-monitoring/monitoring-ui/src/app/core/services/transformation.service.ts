import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {TransformationModelForSnapshotPanel} from '../models/transformation.model';

@Injectable({
  providedIn: 'root'
})
export class TransformationService {
  constructor(private http: HttpClient) {}

  private baseUrl = '/api/transformation';

  getTransformations(
    syncJobId?: string
  ) {
    return this.http.get<TransformationModelForSnapshotPanel[]>(`${this.baseUrl}/${syncJobId}`);
  }


}
