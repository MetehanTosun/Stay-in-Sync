// src/app/models/api-auth.dto.ts

import { ApiAuthConfigurationBase } from './api-auth-configuration-base';

/**
 * BASIC auth payload (username/password).
 */
export interface BasicAuthDTO extends ApiAuthConfigurationBase {
  authType: 'BASIC';
  username: string;
  password: string;
}

/**
 * API-Key auth payload.
 */
export interface ApiKeyAuthDTO extends ApiAuthConfigurationBase {
  authType: 'API_KEY';
  apiKey: string;
  headerName: string;
}