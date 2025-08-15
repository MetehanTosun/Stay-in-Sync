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
          operator: 'eq',
          operandRight: 'asset-newsum-01',
        },
      ],
    },
    {

      '@context': { edc: 'https://w3id.org/edc/v0.0.1/ns/' },
      '@id': 'contract-def-asset-weather-data-02',
      accessPolicyId: 'policy-BPNL000000000001',
      contractPolicyId: 'policy-BPNL000000000001',
      assetsSelector: [
        {
          operandLeft: 'https://w3id.org/edc/v0.0.1/ns/id',
          operator: 'eq',
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

  getOdrlPolicyDefinitions(): Promise<OdrlPolicyDefinition[]> {
    return Promise.resolve([...this.mockOdrlPolicies]);
  }

  getContractDefinitions(): Promise<OdrlContractDefinition[]> {
    return Promise.resolve(this.mockOdrlContractDefinitions);
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
    if (!this.validateContractDefinitionOperator(odrlContractDef)) {
      const errorMessage = `Invalid Contract Definition: One or more asset selectors use an unsupported operator.`;
      console.error(errorMessage, odrlContractDef);
      return Promise.reject(new Error(errorMessage));
    }

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
    if (!this.validateContractDefinitionOperator(odrlContractDef)) {
      const errorMessage = `Invalid Contract Definition: One or more asset selectors use an unsupported operator.`;
      console.error(errorMessage, odrlContractDef);
      return Promise.reject(new Error(errorMessage));
    }

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

  // Helper methods
  private transformOdrlToAccessPolicy(odrlDef: OdrlPolicyDefinition): AccessPolicy {
    const permission = odrlDef.policy?.permission?.[0];
    const constraints = permission?.constraint || [];

    // find the BPN constraint to display in the table.
    const bpnConstraint = constraints.find(c => c.leftOperand === 'BusinessPartnerNumber');

    const firstConstraint = constraints[0];

    return {
      id: odrlDef['@id'],
      bpn: bpnConstraint?.rightOperand || 'N/A', // Show BPN if available, otherwise indicate not applicable.
      action: permission?.action || 'use',
      operator: firstConstraint?.operator || 'N/A',
      contractPolicies: [],
    };
  }

  /**
   * Validates that contract definition use the '=' operator.
   * @param contractDef The contract definition to validate.
   * @returns True if valid, false otherwise.
   */
  private validateContractDefinitionOperator(contractDef: OdrlContractDefinition): boolean {
    if (!contractDef.assetsSelector || contractDef.assetsSelector.length === 0)
    {
      return true;
    }
    // These are the ODRL-compliant operators that the Normal Mode UI supports.
    const allowedOperators = ['eq', 'neq'];
    return contractDef.assetsSelector.every(selector => allowedOperators.includes(selector.operator));
  }
}
