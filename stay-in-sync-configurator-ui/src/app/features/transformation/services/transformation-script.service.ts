import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs/internal/Observable';
import {TransformationScript} from '../models/transformation-script.model';

@Injectable({
  providedIn: 'root'
})

export class TransformationScriptService {

  constructor(readonly httpClient: HttpClient) {}

  getAll(): Observable<TransformationScript[]> {
    return this.httpClient.get<TransformationScript[]>(`/api/config/transformation-script`);
  }

  create(transformationScript: TransformationScript): Observable<TransformationScript> {
    return this.httpClient.post<TransformationScript>(`/api/config/transformation-script`, transformationScript);
  }

}
