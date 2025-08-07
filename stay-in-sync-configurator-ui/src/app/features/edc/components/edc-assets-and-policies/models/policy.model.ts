/**
 * Access Policy
 */
export interface AccessPolicy {
  id: string;
  bpn: string;
  contractPolicies: ContractPolicy[];
  action: string;
  operator: string;
}

/**
 * Contract Policy
 */
export interface ContractPolicy {
  id:string;
  assetId: string;
  bpn: string;
  accessPolicyId: string;
}


export interface OdrlPolicyDefinition {
  '@context': any;
  '@id': string;
  policy: OdrlPolicy;

}

export interface OdrlPolicy {
  permission: OdrlPermission[];
}

export interface OdrlPermission {
  action: string;
  constraint: OdrlConstraint[];
}

export interface OdrlConstraint {
  leftOperand: string;
  operator: string;
  rightOperand: string;
}

/**
 * Represents the payload for creating a new Contract Definition.
 */
export interface OdrlContractDefinition {
  '@context': any;
  '@id': string;
  accessPolicyId: string;
  contractPolicyId: string;
  assetsSelector: OdrlCriterion[];
}

/**
 * A single criterion in the assetsSelector.
 */
export interface OdrlCriterion {
  operandLeft: string;
  operator: string;
  operandRight: string;
}
