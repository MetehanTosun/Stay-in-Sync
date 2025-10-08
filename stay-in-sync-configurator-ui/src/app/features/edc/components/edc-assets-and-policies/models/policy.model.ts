/**
 * ODRL Policy Definition
 */
export interface OdrlPolicyDefinition {
  '@context': any;
  '@id': string;
  policy?: OdrlPolicy;
  permission?: OdrlPermission[];
    bpn?: string;
   
   // UI/Backend-Helper-Felder
  
  dbId?: string;  // neue UUID vom Backend
  policyId?: string; // Business-Key aus DB  
  thirdPartyChanges?: boolean;
}

export interface OdrlPolicy {
  permission: OdrlPermission[];
}

export interface OdrlPermission {
  action: string;
  constraint: OdrlConstraint[];
}

export interface OdrlConstraint {
  leftOperand?: string;
  operator?: string;
  rightOperand?: any; // String oder Array von Strings
  and?: OdrlConstraint[];
  or?: OdrlConstraint[];
}

/**
 * Contract Definition
 */
export interface OdrlContractDefinition {
  bpn: any;
  assetId: any;
  id: any;
  '@context': any;
  '@id': string;
  accessPolicyId: string;
  contractPolicyId: string;
  assetsSelector: OdrlCriterion[];
  thirdPartyChanges?: boolean;
}

/**
 * Criterion für assetsSelector
 */
export interface OdrlCriterion {
  operandLeft: string;
  operator: string;
  operandRight: any; // String oder Array von Strings
}

/**
 * UI-spezifische Contract Definition
 */
export interface UiContractDefinition {
  id: string;
  assetId: string;
  bpn: string;
  accessPolicyId: string;
  thirdPartyChanges?: boolean;
   assetsSelector?: OdrlCriterion[]; // optional, da leer sein kann
}

/**
 * UI-spezifische Policy-Darstellung
 */
export interface UiPolicy {
  id: string;
  bpn?: string;
  action?: string;
}
