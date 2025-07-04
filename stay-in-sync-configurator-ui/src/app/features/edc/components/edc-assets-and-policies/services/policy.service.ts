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

  /**
   * Updates an existing access policy by modifying the underlying ODRL model.
   * In a real backend, this would be an HTTP PUT request.
   */
  updateAccessPolicy(policyToUpdate: AccessPolicy): Promise<void> {
    const index = this.mockOdrlPolicies.findIndex(p => p['@id'] === policyToUpdate.id);
    if (index !== -1) {
      // Find the policy in our mock database and update its properties
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

  /**
   * Deletes an entire access policy from the mock ODRL list.
   * In a real backend, this would be an HTTP DELETE request.
   */
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

  /**
   * In backend, this should be an HTTP PUT request.
   */
  updateContractDefinition(odrlContractDef: OdrlContractDefinition): Promise<void> {
    // This mock service doesn't maintain a separate list of contract definitions.
    // We just log the action and simulate a successful response. The component's
    // UI update logic is what provides the user feedback.
    console.log('Mock Service: Simulating update of contract definition:', JSON.stringify(odrlContractDef, null, 2));

    if (odrlContractDef['@id']) {
      return Promise.resolve(); // Simulate success
    } else {
      return Promise.reject('Contract definition ID is missing');
    }
  }



  /**
   * Deletes a single contract definition.
   * this should be an HTTP DELETE request.
   */
  deleteContractDefinition(contractDefId: string): Promise<void> {
    // Since this mock service doesn't maintain a list of contract definitions,
    // we just log the action and simulate a successful response. The UI handles the removal.
    console.log('Mock Service: Simulating deletion of contract definition with id', contractDefId);

    if (contractDefId) {
      return Promise.resolve(); // Simulate success
    } else {
      return Promise.reject('Contract definition ID not provided');
    }
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
