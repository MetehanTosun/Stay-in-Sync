import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {SourceSystem} from '../models/source-system.model';

@Injectable({ providedIn: 'root' })
export class SourceSystemService {

  constructor(private http: HttpClient) {}

  getAll(): Observable<SourceSystem[]> {
    return this.http.get<SourceSystem[]>(`/api/config/source-system`);
  }
}
