/**
 * The response received from the backend after a test call.
 */
export interface ArcTestCallResponse {
  isSuccess: boolean;
  httpStatusCode: number;
  responsePayload: Record<string, any> | null;
  generatedDts: string | null;
  errorMessage?: string;
}

export interface SourceSystem {
    id: number;
    name: string;
    apiUrl: string;
    description: string;
    apiType: string;
    apiAuthType: string; // currently BASIC, API_KEY
    openApiSpec: string; // TODO: mapping to DTO is byte[], figure something out
}

export interface SourceSystemEndpoint {
    id: number;
    sourceSystemId: number;
    endpointPath: string;
    httpRequestType: string;
}
