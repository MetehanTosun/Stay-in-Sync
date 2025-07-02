import { Injectable } from '@angular/core';
import { AccessPolicy } from '../models/policy.model';
import { of, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class PolicyService {

  private mockAccessPolicies: AccessPolicy[] = [
    {
      id: 'ap-001',
      bpn: 'BPNL000000000001',
      description: 'Company A',
      contractPolicies: [
        { id: 'cp-001a', assetId: 'asset-abc-123' },
        { id: 'cp-001b', assetId: 'asset-def-456' }
      ]
    },
    {
      id: 'ap-002',
      bpn: 'BPNL000000000002',
      description: 'Company B',
      contractPolicies: [
        { id: 'cp-002a', assetId: 'asset-xyz-789' },
        { id: 'cp-002b', assetId: 'asset-xyz-789' },
        { id: 'cp-002c', assetId: 'asset-uvw-101' }
      ]
    },
    {
      id: 'ap-003',
      bpn: 'BPNL000000000003',
      description: 'Company C',
      contractPolicies: []
    }
  ];

  constructor() {}

  getAccessPolicies(): Promise<AccessPolicy[]> {
    return Promise.resolve([...this.mockAccessPolicies]);
  }
}
