export interface TargetSystem {
  id: number;
  name: string;
  apiUrl: string;
  description: string;
  apiType: string;
  openAPI: string; // TODO: check if necessary
  targetSystemEndpointIds: number[];
}

export interface TargetArcConfiguration {
  id: number;
  alias: string;
  targetSystemName: string;
  arcPatternType: 'BASIC_API' | 'OBJECT_UPSERT' | 'LIST_UPSERT' | 'CUSTOM_WORKFLOW';
  actions: TargetArcAction[];
  arcType: 'REST';
}

export interface TargetArcAction {
  endpointId: number;
  endpointPath: string;
  httpMethod: string;
  actionRole: 'CHECK' | 'CREATE' | 'UPDATE';
  executionOrder: number;
}

export interface EndpointSuggestion {
  id: number;
  endpointPath: string;
  httpRequestType: string;
  description?: string;
  //TODO: check for additional display context
}

export interface CreateTargetArcDTO {
    alias: string;
    targetSystemId: number;
    arcPatternType: TargetArcConfiguration['arcPatternType'];
    actions: {
        endpointId: number;
        actionRole: TargetArcAction['actionRole'];
        executionOrder: number;
    }[];
}

export interface CreateAasTargetArcDTO {
  alias: string;
  targetSystemId: number;
  submodelId: number;
}

export interface AasTargetArcConfiguration {
  id: number;
  alias: string;
  targetSystemId: number;
  targetSystemName: string;
  submodelId: number;
  submodelIdShort: string;
  arcType: 'AAS';
}

export type AnyTargetArc = TargetArcConfiguration | AasTargetArcConfiguration;

export interface CreateAasTargetArcDTO {
    alias: string;
    targetSystemId: number;
    submodelId: number;
}

export interface UpdateTransformationRequestConfigurationDTO {
  restTargetArcIds: number[];
  aasTargetArcIds: number[];
}

export interface UpdateTransformationArcsDTO {
  targetArcIds: number[];
}

export interface SubmodelDescription {
  id: number;
  submodelId: string;
  idShort: string;
}

export interface TypeLibrary {
  filePath: string;
  content: string;
}

export interface TypeDefinitionsResponse {
  libraries: TypeLibrary[];
}
