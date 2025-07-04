// src/app/models/api-request-parameter-message.dto.ts

/**
 * Mirrors backend ApiRequestParameterMessageDTO
 */
export interface ApiRequestParameterMessageDto {
    /** Name of the parameter (e.g., "id") */
    paramName: string;
    /** Value for this parameter */
    paramValue: string;
  }