// src/app/models/api-auth-configuration.dto.ts

import { BasicAuthDTO, ApiKeyAuthDTO } from './api-auth.dto';

/**
 * Union of all concrete auth configurations.
 * This is what your service calls should consume/send.
 */
export type ApiAuthConfigurationDTO = BasicAuthDTO | ApiKeyAuthDTO;