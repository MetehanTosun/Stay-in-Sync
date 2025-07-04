// src/app/models/api-endpoint-query-param-value.dto.ts

/**
 * Mirrors the backend ApiEndpoindQueryParamValueDTO record.
 */
export interface ApiEndpointQueryParamValueDTO {
    /** Unique identifier for this query-param value */
    id: number;
  
    /** ID of the parent query-param definition */
    queryParamId: number;
  
    /** Name of the parameter, e.g. "withName" */
    paramName: string;
  
    /** Value for this parameter, e.g. "Stuttgart" */
    paramValue: string;
  }