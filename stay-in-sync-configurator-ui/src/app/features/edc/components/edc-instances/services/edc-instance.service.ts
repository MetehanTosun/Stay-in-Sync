import { Injectable } from '@angular/core';
import { EdcInstance } from '../models/edc-instance.model';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class EdcInstanceService {


  private mockEdcInstances: EdcInstance[] = [
    {
      id: 'instance-1',
      name: 'Catena-X Dev',
      url: 'https://edc.dev.catena-x.net/management',
      protocolVersion: '1.0.0',
      description: 'Development environment for the Catena-X network. Used for testing and integration.',
      bpn: 'BPNL000000000001',
      apiKey: 'test-api-key-123',
    },
    {
      id: 'instance-2',
      name: 'Internal Test EDC',
      url: 'http://localhost:19193/management',
      protocolVersion: '1.1.0',
      description: 'Local testing instance for internal development and feature validation.',
      bpn: 'BPNL000000000002',
      apiKey: 'local-key-456',
    },
    {
      id: 'instance-3',
      name: 'Partner A Connector',
      url: 'https://partner-a.com/api/v2/data',
      protocolVersion: '1.0.0',
      description: 'Connector for data exchange with Partner A.',
      bpn: 'BPNL000000000ABC',
      // apiKey is optional
    },
  ];

  constructor(private http: HttpClient) {}

  // Method to get EDC instances
  getEdcInstancesLarge(): Promise<EdcInstance[]> {
    return Promise.resolve([...this.mockEdcInstances]); // Return a copy
  }

  getEdcInstancesObservable(): Observable<EdcInstance[]> {
    return of([...this.mockEdcInstances]);
  }

  // Future methods for CRUD operations would go here
}
