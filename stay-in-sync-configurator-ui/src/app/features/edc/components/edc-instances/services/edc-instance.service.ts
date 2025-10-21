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

  private backendUrl = 'http://localhost:8090/api/config/edcs';

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
    // Ensure id is a number when sending to backend
    const numericId = parseInt(id, 10);
    if (isNaN(numericId)) {
      console.error(`Invalid ID format for fetching: ${id}`);
      throw new Error('Invalid ID format');
    }
    return this.http.get<EdcInstance>(`${this.backendUrl}/${numericId}`);
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
  updateEdcInstance(id: string | number | null | undefined, instance: EdcInstance): Observable<EdcInstance> {
    if (this.mockMode) {
      console.warn(`Mock Mode: Updating EDC instance ${id}.`);
      const idToUse = id || `edc-instance-${Date.now()}`;
      const index = MOCK_EDC_INSTANCES.findIndex(i => i.id === id);
      if (index > -1) {
        MOCK_EDC_INSTANCES[index] = { ...instance, id: idToUse };
        return of(MOCK_EDC_INSTANCES[index]).pipe(delay(300));
      }
      return of(instance);
    }

    // If ID is null/undefined, we're creating a new instance
    if (id === null || id === undefined) {
      console.log('No ID provided, treating as a new instance creation');
      return this.createEdcInstance(instance);
    }

    // Ensure id is a number when sending to backend
    const numericId = typeof id === 'number' ? id : parseInt(id.toString(), 10);
    if (isNaN(numericId)) {
      console.error(`Invalid ID format for update: ${id}`);
      throw new Error('Invalid ID format');
    }
    return this.http.put<EdcInstance>(`${this.backendUrl}/${numericId}`, instance);
  }

  // Instanz löschen
  deleteEdcInstance(id: string | number | null | undefined): Observable<void> {
    // Cannot delete without a valid ID
    if (id === null || id === undefined) {
      console.error('Cannot delete instance with null or undefined ID');
      throw new Error('Invalid ID: Cannot delete without a valid ID');
    }

    if (this.mockMode) {
      console.warn(`Mock Mode: Deleting EDC instance ${id}.`);
      const index = MOCK_EDC_INSTANCES.findIndex(i => i.id === id);
      if (index > -1) {
        MOCK_EDC_INSTANCES.splice(index, 1);
      }
      return of(undefined).pipe(delay(300));
    }

    // Ensure id is a number when sending to backend
    const numericId = typeof id === 'number' ? id : parseInt(id.toString(), 10);
    if (isNaN(numericId)) {
      console.error(`Invalid ID format for deletion: ${id}`);
      throw new Error('Invalid ID format');
    }
    
    return this.http.delete<void>(`${this.backendUrl}/${numericId}`);
  }
}
