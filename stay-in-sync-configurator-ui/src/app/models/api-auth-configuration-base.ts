// src/app/models/api-auth-configuration-base.ts

/**
 * Base interface for all auth configurations.
 * Mirrors the backend ApiAuthConfigurationDTO interface.
 */
export interface ApiAuthConfigurationBase {
  /** Discriminator: must be 'BASIC' or 'API_KEY' */
  authType: 'BASIC' | 'API_KEY';
}