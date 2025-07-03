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

  constructor(private http: HttpClient) {}

  getAccessPolicies(): Promise<AccessPolicy[]> {
    const policies = this.mockOdrlPolicies.map(this.transformOdrlToAccessPolicy);
    return Promise.resolve(policies);
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


  /**
   * Creates a new contract definition by posting an ODRL-formatted JSON object.
   * @param odrlContractDef The complete ODRL contract definition object.
   */
  createContractDefinition(odrlContractDef: OdrlContractDefinition): Promise<any> {
    const url = `${this.managementApiUrl}/contractdefinitions`;
    console.log('Posting Contract Definition:', JSON.stringify(odrlContractDef, null, 2));

    // For now, we just simulate a successful API call.
    return Promise.resolve({ message: 'Contract definition created successfully!' });

    /*
    // REAL IMPLEMENTATION
    return lastValueFrom(this.http.post(url, odrlContractDef));
    */
  }

  //helper

  private transformOdrlToAccessPolicy(odrlDef: OdrlPolicyDefinition): AccessPolicy {
    const permission = odrlDef.policy?.permission?.[0];
    const constraint = permission?.constraint?.[0];

    return {
      id: odrlDef['@id'],
      bpn: constraint?.rightOperand || '',
      action: permission?.action || 'use',
      operator: constraint?.operator || 'eq',
      contractPolicies: [],
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
