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
      name: 'EDC 1',
      url: 'https://edc.dev.catena-x.net/management',
      protocolVersion: '1.0.0',
      description: 'lurem ipsum dolor sit',
      bpn: 'BPNL000000000001',
      apiKey: 'test-api-key-123',
    },
    {
      id: 'instance-2',
      name: 'EDC 2',
      url: 'http://localhost:19193/management',
      protocolVersion: '1.1.0',
      description: 'lurem ipsum dolor sit amet, consectetur adipiscing elit.',
      bpn: 'BPNL000000000002',
      apiKey: 'local-key-456',
    },
    {
      id: 'instance-3',
      name: 'EDC 3',
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
