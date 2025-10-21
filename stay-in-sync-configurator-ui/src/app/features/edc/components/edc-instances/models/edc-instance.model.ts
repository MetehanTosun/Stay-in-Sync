export interface EdcInstance {
  id: string | number | null; // Kann String, Nummer oder null sein (null für neue Instanzen)
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
