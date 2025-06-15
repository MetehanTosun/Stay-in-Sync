import { Injectable } from '@angular/core';
import { EdcInstance } from '../models/edc-instance.model';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class EdcInstanceService {

  private mockEdcInstances: EdcInstance[] = [
  ];

  constructor(private http: HttpClient) {}

  // Method to get EDC instances
  getEdcInstancesLarge(): Promise<EdcInstance[]> {

    return Promise.resolve([...this.mockEdcInstances]); // Return a copy
  }


  getEdcInstancesObservable(): Observable<EdcInstance[]> {
    // return this.http.get<EdcInstance[]>('/api/edc-instances');
    return of([...this.mockEdcInstances]);
  }

}
