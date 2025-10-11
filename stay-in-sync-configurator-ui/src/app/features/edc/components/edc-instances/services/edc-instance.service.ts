import { Injectable } from '@angular/core';
import { EdcInstance } from '../models/edc-instance.model';
import { HttpClient } from '@angular/common/http';
import { Observable, of, delay } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class EdcInstanceService {

  private backendUrl = 'http://localhost:8090/api/config/edcs';

  constructor(private http: HttpClient) {}

  // Alle Instanzen laden
  getEdcInstances(): Observable<EdcInstance[]> {
    return this.http.get<EdcInstance[]>(this.backendUrl);
  }

  // Einzelne Instanz holen
  getEdcInstance(id: string): Observable<EdcInstance> {
    return this.http.get<EdcInstance>(`${this.backendUrl}/${id}`);
  }

  // Neue Instanz anlegen
  createEdcInstance(instance: EdcInstance): Observable<EdcInstance> {
    return this.http.post<EdcInstance>(this.backendUrl, instance);
  }

  // Instanz aktualisieren
  updateEdcInstance(id: string, instance: EdcInstance): Observable<EdcInstance> {
    return this.http.put<EdcInstance>(`${this.backendUrl}/${id}`, instance);
  }

  // Instanz löschen
  deleteEdcInstance(id: string): Observable<void> {
    return this.http.delete<void>(`${this.backendUrl}/${id}`);
  }
}
