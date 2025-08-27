import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TargetSystemDTO } from '../models/targetSystemDTO';

@Injectable({ providedIn: 'root' })
export class TargetSystemResourceService {
  constructor(private http: HttpClient) {}

  getAll(): Observable<TargetSystemDTO[]> {
    return this.http.get<TargetSystemDTO[]>(`/api/config/target-systems`);
  }

  getById(id: number): Observable<TargetSystemDTO> {
    return this.http.get<TargetSystemDTO>(`/api/config/target-systems/${id}`);
  }

  create(dto: TargetSystemDTO): Observable<TargetSystemDTO> {
    return this.http.post<TargetSystemDTO>(`/api/config/target-systems`, dto);
  }

  update(id: number, dto: TargetSystemDTO): Observable<TargetSystemDTO> {
    return this.http.put<TargetSystemDTO>(`/api/config/target-systems/${id}`, dto);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/config/target-systems/${id}`);
  }
}


