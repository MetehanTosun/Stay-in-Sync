import { EdcInstance } from '../components/edc-instances/models/edc-instance.model';
import { OdrlContractDefinition } from '../components/edc-assets-and-policies/models/policy.model';
import { TargetSystem } from '../models/target-system.model';

/**
 * Mock EDC Instances
 */
export let MOCK_EDC_INSTANCES: EdcInstance[] = [
  {
    id: 'edc-instance-1',
    name: 'Development Connector (Mock)',
    url: 'http://localhost:19193/management',
    protocolVersion: '1.0.0',
    description: 'Local EDC for development purposes.',
    bpn: 'BPNL000000000DEV',
    apiKey: 'test-api-key',
    edcAssetEndpoint: 'http://localhost:19193/api/assets',
    edcPolicyEndpoint: 'http://localhost:19193/api/policies',
    edcContractDefinitionEndpoint: 'http://localhost:19193/api/contractdefinitions'
  },
  {
    id: 'edc-instance-2',
    name: 'Staging Connector (Mock)',
    url: 'https://edc.staging.example.com/management',
    protocolVersion: '1.0.0',
    description: 'Staging environment connector.',
    bpn: 'BPNL000000000STG',
    apiKey: 'staging-api-key',
    edcAssetEndpoint: 'https://edc.staging.example.com/api/assets',
    edcPolicyEndpoint: 'https://edc.staging.example.com/api/policies',
    edcContractDefinitionEndpoint: 'https://edc.staging.example.com/api/contractdefinitions'
  },
];

/**
 * Mock ODRL Assets (raw format)
 */
export let MOCK_ODRL_ASSETS: Record<string, any[]> = {
  'edc-instance-1': [
    {
      '@id': 'asset-1',
      '@context': { edc: 'https://w3id.org/edc/v0.0.1/ns/' },
      properties: {
        'asset:prop:name': 'Test Asset 1',
        'asset:prop:description': 'A sample asset for testing.',
        'asset:prop:contenttype': 'application/json',
        'asset:prop:version': '1.0',
      },
      dataAddress: {
        type: 'HttpData',
        baseUrl: 'https://jsonplaceholder.typicode.com/todos/1',
        queryParams: 'page=1&limit=10', // Example for query params parsing
        'header:Authorization': 'Bearer MOCK_TOKEN_123', // Example for header parsing
        'header:X-Custom-Header': 'StayInSync'
      }
    },
    {
      '@id': 'asset-2',
      '@context': { edc: 'https://w3id.org/edc/v0.0.1/ns/' },
      properties: {
        'asset:prop:name': 'Another Test Asset',
        'asset:prop:description': 'Another sample asset for testing.',
        'asset:prop:contenttype': 'application/json',
        'asset:prop:version': '1.1',
      },
      dataAddress: {
        type: 'HttpData',
        baseUrl: 'https://jsonplaceholder.typicode.com/posts/1',
      },
    },
  ],
  'edc-instance-2': [],
};

/**
 * Mock Policies (backend DTO format)
 */
export let MOCK_POLICIES: Record<string, any[]> = {
  'edc-instance-1': [
    {
      id: 'db-uuid-policy-1', // DB id
      policyId: 'policy-bpn-dev', // Business key
      policy: {
        '@context': { odrl: 'http://www.w3.org/ns/odrl/2/' },
        '@id': 'policy-bpn-dev',
        permission: [
          {
            action: 'use',
            constraint: [
              {
                leftOperand: 'BusinessPartnerNumber',
                operator: 'eq',
                rightOperand: 'BPNL000000000DEV',
              },
            ],
          },
        ],
      },
    },
  ],
  'edc-instance-2': [],
};

/**
 * Mock Contract Definitions
 */
export let MOCK_CONTRACT_DEFINITIONS: Record<string, OdrlContractDefinition[]> = {
  'edc-instance-1': [],
  'edc-instance-2': [],
};

/**
 * Mock Transformations (Target Systems)
 */
export const MOCK_TRANSFORMATIONS: TargetSystem[] = [
  { id: '1', alias: 'User Management API' },
  { id: '2', alias: 'Events API' },
  { id: '3', alias: 'Product Catalog API' }
];

/**
 * Mock Target Arc Configurations
 */
export const MOCK_TARGET_ARC_CONFIGS: { [key: string]: any } = {
  "1": {
    "dataAddress": {
      "baseUrl": "https://api.example.com/users",
      "proxyQueryParams": "true",
      "header:Authorization": "Bearer <YOUR_USER_API_TOKEN>"
    }
  },
  "2": {
    "dataAddress": {
      "baseUrl": "https://api.example.com/events",
      "queryParams": "activeOnly=true&include=details",
      "header:Accept": "application/json"
    }
  },
  "3": {
    "dataAddress": {} // Example of a system with no default config
  }
};
