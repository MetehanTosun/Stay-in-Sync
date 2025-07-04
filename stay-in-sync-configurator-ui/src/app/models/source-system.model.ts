// src/app/models/source-system.model.ts

import { SourceSystemDTO } from './source-system.dto';

/**
 * Domain model for a SourceSystem.
 * Instantiated from a DTO, and can hold UI logic/helper methods.
 */
export class SourceSystem {
  id: number;
  name: string;
  apiUrl: string;
  description?: string;
  apiType: string;
  openApiSpec?: ArrayBuffer | string;

  constructor(dto: SourceSystemDTO) {
    this.id          = dto.id;
    this.name        = dto.name;
    this.apiUrl      = dto.apiUrl;
    this.description = dto.description;
    this.apiType     = dto.apiType;
    this.openApiSpec = dto.openApiSpec;
  }

  /** Example helper: formatted display name */
  get displayName(): string {
    return `${this.name} [${this.apiType}]`;
  }
}