// src/app/features/source-system/services/aas.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {SourceSystem} from '../models/source-system.model';

export interface AasInstance {
  id: string;
  name: string;
}

@Injectable({ providedIn: 'root' })
export class AasService {

  constructor(private http: HttpClient) {}

  getAll(): Observable<SourceSystem[]> {
    return this.http.get<SourceSystem[]>(`/api/aas`);
  }
}
