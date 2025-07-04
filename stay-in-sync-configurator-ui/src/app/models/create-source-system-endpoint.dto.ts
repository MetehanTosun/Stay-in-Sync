// src/app/models/create-source-system-endpoint.dto.ts

/**
 * DTO for creating a new endpoint on a SourceSystem.
 * Mirrors the backend CreateSourceSystemEndpointDTO record.
 */
export interface CreateSourceSystemEndpointDTO {
    /** The path of the endpoint, e.g. "/pets/{id}" */
    endpointPath: string;
    /** HTTP method for the endpoint, e.g. "GET", "POST", "PUT", "DELETE" */
    httpRequestType: string;
  }