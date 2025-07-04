// Mirrors the ApiAuthType enum in the backend
export type ApiAuthType = 'BASIC' | 'API_KEY';

// Payload for BASIC auth (username + password)
export interface BasicAuthDTO {
  authType: 'BASIC';
  username: string;
  password: string;
}

// Payload for API_KEY auth (apiKey + headerName)
export interface ApiKeyAuthDTO {
  authType: 'API_KEY';
  apiKey: string;
  headerName: string;
}

// Union type for polymorphic authConfig
export type ApiAuthConfigurationDTO = BasicAuthDTO | ApiKeyAuthDTO;

/**
 * DTO for creating a new SourceSystem.
 * - `id` is optional during creation.
 * - `openApiSpec` can be a File (for multipart upload) or a Base64/string.
 */
export interface CreateSourceSystemDTO {
  id?: number;
  name: string;
  apiUrl: string;
  description?: string;
  apiType: string;
  apiAuthType: ApiAuthType;
  authConfig?: ApiAuthConfigurationDTO;
  /** For file uploads: a File object or a Base64-encoded string */
  openApiSpec?: File | string;
}