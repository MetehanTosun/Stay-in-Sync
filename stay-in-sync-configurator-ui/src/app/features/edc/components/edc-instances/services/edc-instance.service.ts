import { Injectable } from '@angular/core';
import { EdcInstance } from '../models/edc-instance.model';
import { HttpClient } from '@angular/common/http';
import { Observable, of, delay } from 'rxjs';
import { MOCK_EDC_INSTANCES } from '../../../mocks/mock-data';

@Injectable({
  providedIn: 'root'
})
export class EdcInstanceService {

  // UI Testing method. To use the real backend, change this to false!
  private mockMode = false;

  private backendUrl = '/api/config/edcs';

  constructor(private http: HttpClient) {}

  // Alle Instanzen laden
  getEdcInstances(): Observable<EdcInstance[]> {
    if (this.mockMode) {
      console.warn('Mock Mode: Fetching EDC instances.');
      return of(MOCK_EDC_INSTANCES).pipe(delay(300));
    }
    return this.http.get<EdcInstance[]>(this.backendUrl);
  }

  // Einzelne Instanz holen
  getEdcInstance(id: string): Observable<EdcInstance> {
    if (this.mockMode) {
      console.warn(`Mock Mode: Fetching EDC instance ${id}.`);
      const instance = MOCK_EDC_INSTANCES.find(i => i.id === id);
      return of(instance!).pipe(delay(300));
    }
    return this.http.get<EdcInstance>(`${this.backendUrl}/${id}`);
  }

  // Neue Instanz anlegen
  createEdcInstance(instance: EdcInstance): Observable<EdcInstance> {
    if (this.mockMode) {
      console.warn('Mock Mode: Creating EDC instance.');
      const newInstance = { ...instance, id: `edc-instance-${Date.now()}` };
      MOCK_EDC_INSTANCES.push(newInstance);
      return of(newInstance).pipe(delay(300));
    }
    return this.http.post<EdcInstance>(this.backendUrl, instance);
  }

  // Instanz aktualisieren
  updateEdcInstance(id: string, instance: EdcInstance): Observable<EdcInstance> {
    if (this.mockMode) {
      console.warn(`Mock Mode: Updating EDC instance ${id}.`);
      const index = MOCK_EDC_INSTANCES.findIndex(i => i.id === id);
      if (index > -1) {
        MOCK_EDC_INSTANCES[index] = { ...instance, id };
        return of(MOCK_EDC_INSTANCES[index]).pipe(delay(300));
      }
      return of(instance);
    }
    return this.http.put<EdcInstance>(`${this.backendUrl}/${id}`, instance);
  }

  // Instanz löschen
  deleteEdcInstance(id: string): Observable<void> {
    if (this.mockMode) {
      console.warn(`Mock Mode: Deleting EDC instance ${id}.`);
      const index = MOCK_EDC_INSTANCES.findIndex(i => i.id === id);
      if (index > -1) {
        MOCK_EDC_INSTANCES.splice(index, 1);
      }
      return of(undefined).pipe(delay(300));
    }
    return this.http.delete<void>(`${this.backendUrl}/${id}`);
  }
}
