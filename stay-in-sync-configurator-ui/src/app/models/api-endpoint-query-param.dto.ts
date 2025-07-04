// src/app/models/api-endpoint-query-param.dto.ts

import { ApiEndpointQueryParamType } from './api-endpoint-query-param-type.model'; 
// oder passe den Pfad an, wenn Du Deine Enums woanders hast

/**
 * DTO für einen einzelnen Query-Parameter mit allen möglichen Werten.
 * Entspricht dem Java-Record ApiEndpointQueryParamDTO.
 */
export interface ApiEndpointQueryParamDTO {
  /** Name des Parameters */
  paramName: string;
  /** Typ des Parameters (z.B. PATH, QUERY, HEADER, ...) */
  queryParamType: ApiEndpointQueryParamType;
  /** Datenbank-ID des Param-Definitionseintrags */
  id: number;
  /** Alle möglichen oder aktuell gesetzten Werte */
  values: string[];
}