// Mirrors the ApiAuthType enum in the backend

import { ApiAuthConfigurationDTO } from './api-auth-configuration.dto';
import { ApiAuthConfigurationBase } from './api-auth-configuration-base';


export type ApiAuthType = 'NONE' | 'BASIC' | 'API_KEY';


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

