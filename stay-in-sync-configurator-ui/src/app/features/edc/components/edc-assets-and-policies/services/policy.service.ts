import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { lastValueFrom, map } from 'rxjs';
import {
  AccessPolicy,
  OdrlContractDefinition,
  OdrlPolicyDefinition,
} from '../models/policy.model';

@Injectable({
  providedIn: 'root',
})
export class PolicyService {
  private managementApiUrl = '/api/management/v2';

  private mockOdrlPolicies: OdrlPolicyDefinition[] = [
    {
      '@context': { odrl: 'http://www.w3.org/ns/odrl/2/' },
      '@id': 'policy-BPNL000000000001',
      policy: {
        permission: [
          {
            action: 'use',
            constraint: [
              {
                leftOperand: 'BusinessPartnerNumber',
                operator: 'eq',
                rightOperand: 'BPNL000000000001',
              },
            ],
          },
        ],
      },
    },
    {
      '@context': { odrl: 'http://www.w3.org/ns/odrl/2/' },
      '@id': 'policy-BPNL000000000002',
      policy: {
        permission: [
          {
            action: 'use',
            constraint: [
              {
                leftOperand: 'BusinessPartnerNumber',
                operator: 'eq',
                rightOperand: 'BPNL000000000002',
              },
            ],
          },
        ],
      },
    },
  ];


  private mockOdrlContractDefinitions: OdrlContractDefinition[] = [
    {

      '@context': { edc: 'https://w3id.org/edc/v0.0.1/ns/' },
      '@id': 'contract-def-asset-newsum-01',
      accessPolicyId: 'policy-BPNL000000000001',
      contractPolicyId: 'policy-BPNL000000000001',
      assetsSelector: [
        {
          operandLeft: 'https://w3id.org/edc/v0.0.1/ns/id',
          operator: '=',
          operandRight: 'asset-newsum-01',
        },
      ],
    },
    {

      '@context': { edc: 'https://w3id.org/edc/v0.0.1/ns/' },
      '@id': 'contract-def-asset-weather-data-02',
      accessPolicyId: 'policy-BPNL000000000001', // Belongs to the first policy
      contractPolicyId: 'policy-BPNL000000000001',
      assetsSelector: [
        {
          operandLeft: 'https://w3id.org/edc/v0.0.1/ns/id',
          operator: '=',
          operandRight: 'asset-weather-data-02',
        },
      ],
    },
  ];


  constructor(private http: HttpClient) {}

  getAccessPolicies(): Promise<AccessPolicy[]> {
    const policies = this.mockOdrlPolicies.map(this.transformOdrlToAccessPolicy);
    return Promise.resolve(policies);
  }


  getContractDefinitions(): Promise<OdrlContractDefinition[]> {
    return Promise.resolve(this.mockOdrlContractDefinitions);
  }


  createAccessPolicy(accessPolicy: AccessPolicy): Promise<any> {
    const odrlPayload = this.transformAccessPolicyToOdrl(accessPolicy);
    return this.uploadPolicyDefinition(odrlPayload);
  }

  uploadPolicyDefinition(odrlPolicy: OdrlPolicyDefinition): Promise<any> {
    console.log('Uploading Policy Definition:', JSON.stringify(odrlPolicy, null, 2));

    const index = this.mockOdrlPolicies.findIndex(p => p['@id'] === odrlPolicy['@id']);
    if (index !== -1) {
      this.mockOdrlPolicies[index] = odrlPolicy;
    } else {
      this.mockOdrlPolicies.unshift(odrlPolicy);
    }

    return Promise.resolve({ message: 'Policy created successfully!' });
  }


  createContractDefinition(odrlContractDef: OdrlContractDefinition): Promise<any> {
    console.log('Posting Contract Definition:', JSON.stringify(odrlContractDef, null, 2));
    const index = this.mockOdrlContractDefinitions.findIndex(cd => cd['@id'] === odrlContractDef['@id']);
    if (index !== -1) {
      this.mockOdrlContractDefinitions[index] = odrlContractDef; // Update if exists
    } else {
      this.mockOdrlContractDefinitions.unshift(odrlContractDef); // Add if new
    }
    return Promise.resolve({ message: 'Contract definition created successfully!' });
  }

  updateContractDefinition(odrlContractDef: OdrlContractDefinition): Promise<void> {
    console.log('Mock Service: Simulating update of contract definition:', JSON.stringify(odrlContractDef, null, 2));
    const index = this.mockOdrlContractDefinitions.findIndex(cd => cd['@id'] === odrlContractDef['@id']);
    if (index !== -1) {
      this.mockOdrlContractDefinitions[index] = odrlContractDef;
      return Promise.resolve();
    } else {
      return Promise.reject('Contract definition not found');
    }
  }

  deleteContractDefinition(contractDefId: string): Promise<void> {
    console.log('Mock Service: Simulating deletion of contract definition with id', contractDefId);
    const initialLength = this.mockOdrlContractDefinitions.length;
    this.mockOdrlContractDefinitions = this.mockOdrlContractDefinitions.filter(cd => cd['@id'] !== contractDefId);

    if (this.mockOdrlContractDefinitions.length < initialLength) {
      return Promise.resolve();
    } else {
      return Promise.reject('Contract definition ID not provided');
    }
  }


  updateAccessPolicy(policyToUpdate: AccessPolicy): Promise<void> {
    const index = this.mockOdrlPolicies.findIndex(p => p['@id'] === policyToUpdate.id);
    if (index !== -1) {
      const odrlPolicy = this.mockOdrlPolicies[index];
      const permission = odrlPolicy.policy.permission[0];
      const constraint = permission.constraint[0];

      permission.action = policyToUpdate.action;
      constraint.operator = policyToUpdate.operator;
      constraint.rightOperand = policyToUpdate.bpn;

      console.log('Mock Service: Updated access policy', this.mockOdrlPolicies[index]);
      return Promise.resolve();
    } else {
      console.error('Mock Service: Access policy not found for update', policyToUpdate);
      return Promise.reject('Access policy not found');
    }
  }

  deleteAccessPolicy(policyId: string): Promise<void> {
    const initialLength = this.mockOdrlPolicies.length;
    this.mockOdrlPolicies = this.mockOdrlPolicies.filter(p => p['@id'] !== policyId);

    if (this.mockOdrlPolicies.length < initialLength) {
      console.log('Mock Service: Deleted access policy with id', policyId);
      return Promise.resolve();
    } else {
      console.error('Mock Service: Access policy not found for deletion', policyId);
      return Promise.reject('Access policy not found');
    }
  }

  // Helper methods are unchanged
  private transformOdrlToAccessPolicy(odrlDef: OdrlPolicyDefinition): AccessPolicy {
    const permission = odrlDef.policy?.permission?.[0];
    const constraint = permission?.constraint?.[0];

    return {
      id: odrlDef['@id'],
      bpn: constraint?.rightOperand || '',
      action: permission?.action || 'use',
      operator: constraint?.operator || 'eq',
      contractPolicies: [], // This will be populated by the component
    };
  }

  private transformAccessPolicyToOdrl(accessPolicy: AccessPolicy): OdrlPolicyDefinition {
    const policyId = accessPolicy.id || `policy-${accessPolicy.bpn}`;

    return {
      '@context': { odrl: 'http://www.w3.org/ns/odrl/2/' },
      '@id': policyId,
      policy: {
        permission: [
          {
            action: accessPolicy.action || 'use',
            constraint: [
              {
                leftOperand: 'BusinessPartnerNumber',
                operator: accessPolicy.operator || 'eq',
                rightOperand: accessPolicy.bpn,
              },
            ],
          },
        ],
      },
    };
  }
}
