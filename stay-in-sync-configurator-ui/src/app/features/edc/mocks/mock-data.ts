import { EdcInstance } from '../components/edc-instances/models/edc-instance.model';
import { OdrlContractDefinition } from '../components/edc-assets-and-policies/models/policy.model';
import { Transformation } from '../models/transformation.model';

import { Template } from '../components/templates/models/template.model';


/**
 * Mock Templates
 */
export const MOCK_TEMPLATES: Template[] = [
  {
    id: `template-1`,
    name: 'Default BPN Access Policy',
    description: 'A simple policy to grant access based on a Business Partner Number.',
    content: {
      "@context": {"odrl": "http://www.w3.org/ns/odrl/2/"},
      "@id": "POLICY_ID_BPN",
      "policy": {
        "permission": [{
          "action": "${Action|use,read,write}",
          "constraint": [{
            "leftOperand": "BusinessPartnerNumber",
            "operator": "${Operator|eq,neq}",
            "rightOperand": "${BPN-Value}"
          }]
        }]
      }
    }
  },
  {
    id: `template-2`,
    name: 'CX Membership Policy',
    description: 'A Catena-X policy that checks for active membership and a BPN.',
    content: {
      "@context": [
        "http://www.w3.org/ns/odrl.jsonld",
        "https://w3id.org/catenax/2025/9/policy/context.jsonld"
      ],
      "@type": "Set",
      "@id": "POLICY_ID_BPN",
      "permission": [
        {
          "action": "${Action|access,read,write,use}",
          "constraint": [
            {
              "leftOperand": "Membership",
              "operator": "${Operator|eq,neq}",
              "rightOperand": "${Status|active,inactive}"
            }
          ]
        }
      ]
    }
  },
];

/**
 * Mock EDC Instances
 */
export let MOCK_EDC_INSTANCES: EdcInstance[] = [
  {
    id: 'edc-instance-1',
    name: 'Development Connector (Mock)',
    controlPlaneManagementUrl: 'http://localhost:19193/management',
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
    controlPlaneManagementUrl: 'https://edc.staging.example.com/management',
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
export const MOCK_TRANSFORMATIONS: Transformation[] = [
  { id: '1', name: 'User Management API', alias: 'User Management API' },
  { id: '2', name: 'Events API', alias: 'Events API' },
  { id: '3', name: 'Product Catalog API', alias: 'Product Catalog API' }
];

/**
 * Mock Target Arc Configurations
 */
export const MOCK_TARGET_ARC_CONFIGS: { [key: string]: any } = {
  '1': {
    id: 101,
    alias: 'Get All Users',
    actions: [{
      name: 'Get All Users Action',
      description: 'Fetches a list of all users from the system.',
      httpMethod: 'GET',
      path: '/users',
      headers: [
        {key: 'Accept', value: 'application/json'}
      ],
      queryParameters: [
        {key: 'page', value: '1'},
        {key: 'limit', value: '100'}
      ]
    }]
  },
  '2': {
    id: 102,
    alias: 'Create New Event',
    actions: [{
      name: 'Create Event Action',
      description: 'Creates a new event in the calendar.',
      httpMethod: 'POST',
      path: '/events',
      headers: [
        { key: 'Content-Type', value: 'application/json' },
        { key: 'Authorization', value: 'Bearer <YOUR_EVENT_API_TOKEN>' }
      ],
      queryParameters: []
    }]
  },
  '3': {
    id: 103,
    alias: 'Product Catalog API',
    actions: [{
      name: 'Get Product by SKU',
      description: 'Retrieves product details for a given SKU.',
      httpMethod: 'GET',
      path: '/products/{sku}',
      headers: [],
      queryParameters: []
    }]
  }
};

/**
 * Mock Target Systems
 */
export const MOCK_TARGET_SYSTEMS: Transformation[] = [
  { id: '10', name: 'CRM Backend', alias: 'CRM Backend' },
  { id: '11', name: 'Inventory Management', alias: 'Inventory Management' },
];

/**
 * Mock Synced Assets
 */
export const MOCK_SYNCED_ASSETS: { [key: string]: any } = {
  '1': {
    "asset:prop:name": "User List Asset",
    "asset:prop:description": "Provides a list of all active users.",
    "asset:prop:version": "1.1.0",
    "asset:prop:contenttype": "application/json"
  },
  '2': {
    "asset:prop:name": "Event Creation Asset",
    "asset:prop:description": "An asset for creating new calendar events.",
    "asset:prop:version": "2.0.0",
    "asset:prop:contenttype": "application/json"
  },
  '3': {
    "asset:prop:name": "Product Details Asset",
    "asset:prop:description": "Provides detailed information about a single product.",
    "asset:prop:version": "1.0.0",
    "asset:prop:contenttype": "application/json"
  }
};
