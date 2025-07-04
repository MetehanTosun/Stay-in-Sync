// src/app/models/create-request-configuration.dto.ts

/**
 * DTO for creating a new Request Configuration.
 */
export interface CreateRequestConfigurationDto {
    /** The name of the configuration */
    name: string;
  
    /** Whether the configuration is active */
    active: boolean;
  
    /** Polling interval in milliseconds */
    pollingIntervalTimeInMs: number;
  }