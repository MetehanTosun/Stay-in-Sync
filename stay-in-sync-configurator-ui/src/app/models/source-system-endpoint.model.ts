// src/app/models/source-system-endpoint.model.ts

/** Repräsentiert einen Endpoint eines Source Systems */
export interface SourceSystemEndpointDto {
    /** Eindeutige ID des Endpoints */
    id: number;
  
    /** Pfad des Endpoints, z. B. "/items" */
    endpointPath: string;
  
    /** HTTP-Methode */
    httpRequestType: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  
    /** Polling aktiviert? */
    pollingActive: boolean;
  
    /** Polling-Intervall in Millisekunden */
    pollingRateInMs: number;
  
    /** JSON-Schema, wenn automatisch extrahiert */
    jsonSchema?: string;
  
    /** Modus der Schema-Erstellung: "auto" oder "manual" */
    schemaMode?: 'auto' | 'manual';
  }
  
  /** DTO zum Anlegen eines neuen Endpoints */
  export interface CreateSourceSystemEndpointDto {
    endpointPath: string;
    httpRequestType: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
    pollingActive: boolean;
    pollingRateInMs: number;
  }