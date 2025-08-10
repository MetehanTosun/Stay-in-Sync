export interface TargetSystemEndpointDTO {
  id?: number;
  targetSystemId: number;
  endpointPath: string;
  httpRequestType: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  description?: string;
  jsonSchema?: string;
}


