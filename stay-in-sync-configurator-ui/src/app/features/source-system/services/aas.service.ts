// src/app/features/source-system/services/aas.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AasInstance {
  id: string;
  name: string;
}

@Injectable({ providedIn: 'root' })
export class AasService {
  private baseUrl = '/aasServer';

  constructor(private http: HttpClient) {}

  getAll(): Observable<AasInstance[]> {
    return this.http.get<AasInstance[]>(`${this.baseUrl}/shells`);
  }
}