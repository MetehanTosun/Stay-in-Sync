export interface EdcInstance {
  id: string;
  name: string;
  url: string;
  protocolVersion: string;
  description: string;
  bpn: string;
  apiKey?: string;
  edcAssetEndpoint: string;
  edcPolicyEndpoint: string;
  edcContractDefinitionEndpoint: string; 
}
