
// --- Data Transfer Objects (DTOs) for API Communication ---

import {SourceSystem} from "../../source-system/models/sourceSystem";

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
  sourceSystemName: string;
  arcType: 'REST';
  endpointId: number;
  endpointPath: string;
  httpMethod: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  responseDts: string;
  responseIsArray: boolean;
  // TODO: Include other relevant fields as needed for display or cloning.
}

export type ArcMap = Record<string, AnyArc[]>;

/**
 * The payload sent to the backend to create or update an ARC.
 */
export interface ArcSaveRequest {
  id?: number;
  alias: string;
  sourceSystemId: number;
  endpointId: number;
  pathParameterValues: Record<string, string>;
  queryParameterValues: Record<string, string>;
  headerValues: Record<string, string>;
  responseDts: string;
  pollingIntervallTimeInMs: number;
}

/**
 * Represents one mapping of an endpoint param to its value.
 */
export interface EndpointParameterDefinition {
  name: string;
  in: 'path' | 'query' | 'header';
  description: string;
  required: boolean;
  options: string[];
  type: 'string' | 'number' | 'integer' | 'boolean' | 'array';
}

export interface SourceSystemApiRequestConfiguration extends ApiRequestConfiguration {
  transformations: string[]; // IDs oder Namen der Transformationen
  sourceSystem: SourceSystem; // Name oder ID des SourceSystems
  sourceSystemEndpoint: string; // Name oder ID des Endpoints
  responseDts: string;
  pollingIntervallTimeInMs: number;
}

export interface AasArc {
  id: number;
  alias: string;
  sourceSystemName: string;
  responseDts: string;
  arcType: 'AAS';
  submodelId: number;
  submodelIdShort: string;
  pollingIntervallTimeInMs: number;
  active: boolean;
}

export type AnyArc = ApiRequestConfiguration | AasArc;

export interface SubmodelDescription {
  id: number;
  submodelId: string;
  idShort: string;
}

export interface AasArcSaveRequest {
  id?: number;
  alias: string;
  sourceSystemId: number;
  submodelId: number;
  pollingIntervallTimeInMs: number;
  active: boolean;
}

export interface ApiHeaderDefinition {
  id: number;
  headerName: string;
  values: string[];
}

export interface ArcWizardContextData {
  pathParams: EndpointParameterDefinition[];
  queryParamDefinitions: EndpointParameterDefinition[];
  headerDefinitions: ApiHeaderDefinition[];
}

export interface ApiEndpointParamDTO {
  paramName: string;
  queryParamType: 'PATH' | 'QUERY';
  id: number;
  description?: string;
  required?: boolean;
  values: string[];
  schemaType: 'string' | 'number' | 'integer' | 'boolean' | 'array';
}
