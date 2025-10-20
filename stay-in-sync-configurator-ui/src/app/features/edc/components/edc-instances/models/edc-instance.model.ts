export interface EdcInstance {
  id: string;
  name: string;
  controlPlaneManagementUrl: string; // Geändert von url zu controlPlaneManagementUrl, um mit dem Backend übereinzustimmen
  protocolVersion: string;
  description: string;
  bpn: string;
  apiKey?: string;
  edcAssetEndpoint?: string;
  edcPolicyEndpoint?: string;
  edcContractDefinitionEndpoint?: string; 
}
