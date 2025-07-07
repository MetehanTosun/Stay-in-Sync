import { Injectable } from '@angular/core';
import {Observable} from 'rxjs/internal/Observable';
import {HttpClient} from '@angular/common/http';
import {Transformation} from '../models/transformation.model';

@Injectable({
  providedIn: 'root'
})
export class TransformationService {

  constructor(readonly httpClient: HttpClient){}

  getAll(): Observable<Transformation[]> {
    return this.httpClient.get<Transformation[]>(`/api/config/transformation`);
  }

  create(transformation: Transformation): Observable<Transformation> {
    return this.httpClient.post<Transformation>(`/api/config/transformation`, transformation);
  }

  delete(transformation: Transformation): Observable<void> {
    return this.httpClient.delete<void>(`/api/config/transformation/${transformation.id}`);
  }

  update(transformation: Transformation): Observable<Transformation> {
    return this.httpClient.put<Transformation>(`/api/config/transformation/${transformation.id}`, transformation);
  }

  getById(id: number): Observable<Transformation> {
    return this.httpClient.get<Transformation>(`/api/config/transformation/${id}`);
  }


}
