export interface CreateTargetSystemEndpointDTO {
  endpointPath: string;
  httpRequestType: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  description?: string;
  jsonSchema?: string;
}


