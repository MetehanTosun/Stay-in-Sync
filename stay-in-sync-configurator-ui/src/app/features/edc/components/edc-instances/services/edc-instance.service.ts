// src/app/features/edc/services/edc-instance.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, firstValueFrom, map } from 'rxjs';
import { EdcInstance } from '../models/edc-instance.model';

// ---------- Backend-Typen ----------
type BackendEdcInstanceDto = {
  id: string;
  name: string;
  url: string;
  protocolVersion?: string;
  description?: string;
  bpn?: string;
  // apiKey wird vom Backend idealerweise NICHT im GET geliefert
};

type BackendEdcInstanceRequest = {
  name: string;
  url: string;
  protocolVersion?: string;
  description?: string;
  bpn?: string;
  apiKey?: string; // write-only
};

// ---------- Adapter ----------
const toUi = (d: BackendEdcInstanceDto): EdcInstance => ({
  id: d.id,
  name: d.name,
  url: d.url,
  protocolVersion: d.protocolVersion ?? '—',
  description: d.description ?? '—',
  bpn: d.bpn ?? '—',
  apiKey: undefined, // nie aus GET anzeigen
});

const toRequest = (m: Partial<EdcInstance>): BackendEdcInstanceRequest => ({
  name: m.name!, // Pflicht
  url: m.url!,   // Pflicht
  protocolVersion: m.protocolVersion && m.protocolVersion !== '—' ? m.protocolVersion : undefined,
  description:     m.description     && m.description     !== '—' ? m.description     : undefined,
  bpn:             m.bpn             && m.bpn             !== '—' ? m.bpn             : undefined,
  apiKey: m.apiKey || undefined,
});

@Injectable({ providedIn: 'root' })
export class EdcInstanceService {
  // In Dev ideal: Proxy nutzen und hier '/api/config/edcs'
  private baseUrl = 'http://localhost:8090/api/config/edcs';

  constructor(private http: HttpClient) {}

  // ---- READ ----
  getAll$(): Observable<EdcInstance[]> {
    return this.http.get<BackendEdcInstanceDto[]>(this.baseUrl).pipe(
      map(list => list.map(toUi))
    );
  }

  getAll(): Promise<EdcInstance[]> {
    return firstValueFrom(this.getAll$());
  }

  // ✅ Alias für bestehende Aufrufer mit .then(...)
  getEdcInstancesLarge(): Promise<EdcInstance[]> {
    return this.getAll();
  }

  getById$(id: string): Observable<EdcInstance> {
    return this.http.get<BackendEdcInstanceDto>(`${this.baseUrl}/${id}`).pipe(map(toUi));
  }

  // ---- CREATE ----
  create$(model: Partial<EdcInstance>): Observable<EdcInstance> {
    return this.http.post<BackendEdcInstanceDto>(this.baseUrl, toRequest(model)).pipe(map(toUi));
  }

  // ---- UPDATE ----
  update$(id: string, model: Partial<EdcInstance>): Observable<EdcInstance> {
    return this.http.put<BackendEdcInstanceDto>(`${this.baseUrl}/${id}`, toRequest(model)).pipe(map(toUi));
  }

  // ---- DELETE ----
  delete$(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
