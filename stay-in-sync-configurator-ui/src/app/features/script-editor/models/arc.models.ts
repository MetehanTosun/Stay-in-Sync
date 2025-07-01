// --- Data Transfer Objects (DTOs) for API Communication ---

/**
 * The payload sent to the backend to test an API call in the ARC wizard.
 */
export interface ArcTestCallRequest {
  sourceSystemId: number;
  endpointId: number;
  pathParameters: Record<string, string | number>;
  queryParameterValues: Record<string, string | number>;
  headerValues: Record<string, string>;
}

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

/**
 * Represents a simplified ApiRequestConfiguration for listing and display purposes.
 * This should match the structure returned by your backend list endpoints.
 */
export interface ApiRequestConfiguration {
  id: number;
  alias: string;
  sourceSystemId: number;
  endpointId: number;
  endpointPath: string;
  httpMethod: string;
  responseDts: string;
  // TODO: Include other relevant fields as needed for display or cloning.
}

/**
 * The payload sent to the backend to create or update an ARC.
 */
export interface ArcSaveRequest {
  id?: number;
  alias: string;
  sourceSystemId: number;
  endpointId: number;
  pathParameters: Record<string, string>;
  queryParameterValues: Record<string, string>;
  headerValues: Record<string, string>;
  responseDts: string;
}
