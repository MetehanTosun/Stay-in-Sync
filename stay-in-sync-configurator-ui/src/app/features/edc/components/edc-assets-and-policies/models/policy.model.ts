
/**
 * Represents an Access Policy
 */
export interface AccessPolicy {
  id: string;
  bpn: string;
  description: string;
  contractPolicies: ContractPolicy[];
}



/**
 * Represents a Contract Policy
 */
export interface ContractPolicy {
  id: string;
  assetId: string;
}

