export interface TargetSystemEndpointDTO {
  id?: number;
  targetSystemId: number;
  endpointPath: string;
  httpRequestType: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  requestBodySchema?: string;
  responseBodySchema?: string;
  responseDts?: string;
  description?: string;
  jsonSchema?: string;
}


