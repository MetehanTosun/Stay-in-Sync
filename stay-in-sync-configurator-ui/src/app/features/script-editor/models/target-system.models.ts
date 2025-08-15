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
  staticHeaderValues: { [key: string]: string };
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
    staticHeaderValues?: { [key: string]: string };
}

export interface UpdateTransformationArcsDTO {
  arcIds: number[];
}

export interface TypeLibrary {
  filePath: string;
  content: string;
}

export interface TypeDefinitionsResponse {
  libraries: TypeLibrary[];
}
