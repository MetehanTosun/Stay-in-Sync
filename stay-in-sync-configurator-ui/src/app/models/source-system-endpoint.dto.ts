// src/app/models/source-system-endpoint.dto.ts

/**
 * DTO mirroring the backend SourceSystemEndpointDTO record.
 * Used for all HTTP requests/responses involving a SourceSystem’s endpoints.
 */
export interface SourceSystemEndpointDTO {
    /** Unique identifier for this endpoint */
    id: number;
    /** Identifier of the parent SourceSystem */
    sourceSystemId: number;
    /** The path of the endpoint, e.g. "/pets/{id}" */
    endpointPath: string;
    /** HTTP method for the endpoint, e.g. "GET", "POST", "PUT", "DELETE" */
    httpRequestType: string;
  }