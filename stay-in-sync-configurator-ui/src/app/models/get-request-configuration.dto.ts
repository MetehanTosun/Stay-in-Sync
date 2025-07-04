// src/app/models/get-request-configuration.dto.ts

import { ApiHeaderDto } from './api-header.dto';
import { ApiRequestParameterMessageDto } from './api-request-parameter-message.dto';

/**
 * Mirrors backend GetRequestConfigurationDTO
 */
export interface GetRequestConfigurationDto {
  /** The name of the configuration */
  name: string;

  /** Whether this config has been used */
  used: boolean;

  /** Polling interval in milliseconds */
  pollingIntervallTimeInMs: number;

  /** Predefined headers for this request configuration */
  apiRequestHeaders: ApiHeaderDto[];

  /** Predefined query parameters for this request configuration */
  apiRequestParameters: ApiRequestParameterMessageDto[];
}