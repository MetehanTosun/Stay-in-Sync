// src/app/models/source-system.dto.ts

/**
 * DTO mirroring the backend SourceSystemDTO record.
 * Used for all HTTP requests/responses.
 */
export interface SourceSystemDTO {
    /** Database ID */
    id: number;
    /** Unique name of the source system */
    name: string;
    /** Base URL of the API */
    apiUrl: string;
    /** Optional free-text description */
    description?: string;
    /** Type of the source system (e.g. REST_OPENAPI, AAS) */
    apiType: string;
    /** Raw OpenAPI spec as ArrayBuffer or Base64 string */
    openApiSpec?: ArrayBuffer | string;
  }