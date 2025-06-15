import { Injectable } from '@angular/core';
import { EdcInstance } from '../models/edc-instance.model';
import { HttpClient } from '@angular/common/http'; // Optional: for real API calls later
import { Observable, of } from 'rxjs'; // Optional: for real API calls

@Injectable({
  providedIn: 'root' // Provide this service at the root level
})
export class EdcInstanceService {

  // Mock data for now, similar to the example's getData()
  private mockEdcInstances: EdcInstance[] = [
    { id: 1000, name: 'Primary Production EDC', url: 'https://prod.edc.example.com', status: 'Active' },
    { id: 1001, name: 'Development Test EDC', url: 'http://dev.edc.example.com', status: 'Active' },
    { id: 1002, name: 'Staging EDC Cluster', url: 'https://staging.edc.example.com', status: 'Inactive' },
    { id: 1003, name: 'Legacy System Connector', url: 'http://legacy.edc.internal', status: 'Pending' },
    { id: 1004, name: 'Partner Integration EDC', url: 'https://partner.edc.example.com', status: 'Active' },
  ];

  constructor(private http: HttpClient) {} // Inject HttpClient if you plan to fetch data from an API

  // Method to get EDC instances (returns a Promise like the example)
  getEdcInstancesLarge(): Promise<EdcInstance[]> {
    // In a real app, you might fetch this via HTTP:
    // return this.http.get<EdcInstance[]>('/api/edc-instances').toPromise();
    return Promise.resolve([...this.mockEdcInstances]); // Return a copy
  }

  // If you prefer Observables (more common in modern Angular):
  getEdcInstancesObservable(): Observable<EdcInstance[]> {
    // return this.http.get<EdcInstance[]>('/api/edc-instances');
    return of([...this.mockEdcInstances]);
  }

  // Add other methods as needed, e.g., getEdcInstanceById, create, update, delete
}
