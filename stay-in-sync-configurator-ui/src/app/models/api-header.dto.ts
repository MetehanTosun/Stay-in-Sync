// src/app/models/api-header.dto.ts

/** 
 * Mirrors backend enum ApiRequestHeaderType 
 * (adjust to match the exact values in your backend)
 */
export type ApiRequestHeaderType = 
  | 'HEADER_SINGLE'  // example, replace with real values 
  | 'HEADER_MULTI';

/**
 * Mirrors backend record ApiHeaderDTO
 */
export interface ApiHeaderDto {
  /** Database ID */
  id: number;

  /** Type of this header (e.g. SINGLE vs MULTI) */
  headerType: ApiRequestHeaderType;

  /** Name of the HTTP header (e.g. "Authorization") */
  headerName: string;

  /** Allowed values for this header */
  values: string[];
}