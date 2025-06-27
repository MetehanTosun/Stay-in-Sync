// src/app/models/source-system.model.ts
export interface SourceSystemDto {
  id?: number;
  name: string;
  description?: string;
  type: 'AAS' | 'REST_OPENAPI';
  apiUrl: string;
  openApiSpecUrl?: string;
  openApiSpec?: string;
}