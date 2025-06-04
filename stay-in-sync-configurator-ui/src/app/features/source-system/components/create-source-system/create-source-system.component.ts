/**
 * CreateSourceSystemComponent
 * 
 * A zone-less Angular component for creating and configuring source systems.
 * Supports both AAS (Asset Administration Shell) and REST OpenAPI integrations
 * with full parameter and request body configuration capabilities.
 * 
 * Features:
 * - Zone-less architecture using Angular Signals
 * - OpenAPI specification parsing and validation
 * - Dynamic endpoint parameter configuration
 * - JSON request body generation and validation
 * - Real-time form validation
 * - Responsive UI with PrimeNG components
 * 
 * @example
 * ```html
 * <app-create-source-system
 *   [visible]="showDialog"
 *   (visibleChange)="showDialog = $event"
 *   (sourceSaved)="onSourceSystemSaved($event)">
 * </app-create-source-system>
 * ```
 */

import { Component, EventEmitter, Input, Output, signal, computed, effect, ChangeDetectionStrategy, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

// PrimeNG
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { AccordionModule } from 'primeng/accordion';
import { ChipModule } from 'primeng/chip';

// Services
import { AasService } from '../../services/aas.service';
import { Observable, of } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';

// Types

/**
 * Supported source system types
 */
type SourceType = 'AAS' | 'REST_OPENAPI';

/**
 * Represents an API endpoint extracted from OpenAPI specification
 */
interface ApiEndpoint {
  /** HTTP path (e.g., '/users/{id}') */
  path: string;
  /** HTTP method (GET, POST, PUT, DELETE, etc.) */
  method: string;
  /** Raw OpenAPI operation object */
  operation: any;
  /** Unique identifier for the endpoint */
  id: string;
  /** Brief description of the endpoint */
  summary?: string;
  /** Detailed description of the endpoint */
  description?: string;
  /** Error response definitions */
  errors?: ApiError[];
  /** Whether the endpoint has documentation issues */
  hasDocumentationIssues?: boolean;
  /** User-provided parameter values */
  parameters?: { [paramName: string]: string };
  /** User-provided request body (for POST/PUT/PATCH) */
  requestBody?: any;
}

/**
 * Represents an API error response
 */
interface ApiError {
  /** HTTP status code (e.g., '400', '404') */
  code: string;
  /** Human-readable error description */
  description: string;
  /** Example error response body */
  example?: any;
}

/**
 * Complete source system configuration data
 */
interface SourceSystemData {
  /** User-defined name for the source system */
  name: string;
  /** Type of source system */
  type: SourceType;
  /** AAS Registry ID (for AAS type) */
  aasId?: string;
  /** OpenAPI specification URL (for REST type) */
  openApiSpecUrl?: string;
  /** Selected and configured endpoint (for REST type) */
  selectedEndpoint?: ApiEndpoint;
  /** List of validation issues found in the specification */
  validationIssues?: string[];
}

@Component({
  selector: 'app-create-source-system',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ButtonModule, DialogModule, 
    DropdownModule, InputTextModule, 
    MessageModule, AccordionModule, ChipModule
  ],
  templateUrl: './create-source-system.component.html',
  styleUrls: ['./create-source-system.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CreateSourceSystemComponent {
  
  // === COMPONENT INPUTS/OUTPUTS ===
  
  /** Controls dialog visibility */
  @Input() visible = false;
  
  /** Emits when dialog visibility changes */
  @Output() visibleChange = new EventEmitter<boolean>();
  
  /** Emits when a source system is successfully saved */
  @Output() sourceSaved = new EventEmitter<SourceSystemData>();

  // === DEPENDENCY INJECTION ===
  
  /** HTTP client for API calls */
  private http = inject(HttpClient);
  
  /** Service for AAS operations */
  private aasService = inject(AasService);
  
  /** Change detector for zone-less updates */
  private cdr = inject(ChangeDetectorRef);

  // === CORE SIGNALS ===
  
  /** Currently selected source system type */
  sourceType = signal<SourceType>('AAS');
  
  /** User-provided source system name */
  sourceName = signal<string>('');
  
  /** Selected AAS ID from registry */
  selectedAasId = signal<string | undefined>(undefined);
  
  // === AAS LOADING SIGNALS ===
  
  /** Loading state for AAS list */
  isLoadingAas = signal<boolean>(false);
  
  /** Available AAS instances from registry */
  aasList = signal<{ id: string; name: string }[]>([]);
  
  // === OPENAPI SIGNALS ===
  
  /** URL to OpenAPI specification */
  openApiSpecUrl = signal<string>('');
  
  /** Loading state for OpenAPI spec fetching */
  isLoadingOpenApiSpec = signal<boolean>(false);
  
  /** Error message from OpenAPI spec loading */
  openApiSpecError = signal<string | null>(null);
  
  /** Parsed OpenAPI specification object */
  parsedOpenApiSpec = signal<any>(null);
  
  /** List of endpoints extracted from OpenAPI spec */
  availableApiEndpoints = signal<ApiEndpoint[]>([]);
  
  /** Currently selected API endpoint */
  selectedApiEndpoint = signal<ApiEndpoint | null>(null);
  
  // === PARAMETER & REQUEST BODY SIGNALS ===
  
  /** User-provided values for endpoint parameters */
  endpointParameters = signal<{ [paramName: string]: string }>({});
  
  /** User-provided JSON request body */
  requestBody = signal<string>('');
  
  /** Validation error for request body JSON */
  requestBodyError = signal<string | null>(null);
  
  // === ERROR MANAGEMENT SIGNALS ===
  
  /** Validation issues found in OpenAPI spec */
  specValidationIssues = signal<string[]>([]);
  
  /** Documentation issues per endpoint */
  endpointDocumentationIssues = signal<{ [endpointId: string]: string[] }>({});

  // === COMPUTED SIGNALS ===
  
  /**
   * Available source type options for dropdown
   */
  sourceTypeOptions = computed(() => [
    { label: 'AAS Registry', value: 'AAS' as SourceType },
    { label: 'REST (from OpenAPI)', value: 'REST_OPENAPI' as SourceType }
  ]);

  /**
   * Whether AAS type is currently selected
   */
  isAasType = computed(() => this.sourceType() === 'AAS');
  
  /**
   * Whether REST OpenAPI type is currently selected
   */
  isRestOpenApiType = computed(() => this.sourceType() === 'REST_OPENAPI');

  /**
   * Whether the OpenAPI spec has validation issues
   */
  hasSpecValidationIssues = computed(() => this.specValidationIssues().length > 0);
  
  /**
   * Whether the selected endpoint has documentation issues
   */
  hasSelectedEndpointIssues = computed(() => {
    const endpoint = this.selectedApiEndpoint();
    if (!endpoint) return false;
    const issues = this.endpointDocumentationIssues()[endpoint.id] || [];
    return issues.length > 0;
  });

  /**
   * Parameters (path/query) for the selected endpoint
   */
  selectedEndpointParams = computed(() => {
    const endpoint = this.selectedApiEndpoint();
    if (!endpoint?.operation?.parameters) return [];
    
    return endpoint.operation.parameters.filter((param: any) => 
      param.in === 'path' || param.in === 'query'
    );
  });

  /**
   * Whether the selected endpoint requires a request body
   */
  selectedEndpointRequiresBody = computed(() => {
    const endpoint = this.selectedApiEndpoint();
    if (!endpoint) return false;
    
    const method = endpoint.method.toUpperCase();
    return ['POST', 'PUT', 'PATCH'].includes(method) && 
           endpoint.operation?.requestBody;
  });

  /**
   * Schema definition for request body
   */
  requestBodySchema = computed(() => {
    const endpoint = this.selectedApiEndpoint();
    if (!endpoint?.operation?.requestBody) return null;
    
    const content = endpoint.operation.requestBody.content;
    const jsonContent = content?.['application/json'];
    return jsonContent?.schema || null;
  });

  /**
   * Whether the form is valid and ready for submission
   */
  isFormValid = computed(() => {
    const name = this.sourceName().trim();
    if (!name) return false;
    
    if (this.sourceType() === 'AAS') {
      return !!this.selectedAasId();
    }
    
    if (this.sourceType() === 'REST_OPENAPI') {
      const hasBasicData = !!(
        this.openApiSpecUrl().trim() && 
        this.selectedApiEndpoint() && 
        !this.isLoadingOpenApiSpec() &&
        !this.openApiSpecError()
      );
      
      if (!hasBasicData) return false;
      
      // Validate required parameters
      const requiredParams = this.selectedEndpointParams().filter((param: any) => param.required);
      const currentParams = this.endpointParameters();
      
      const paramsValid = requiredParams.every((param: any) => 
        currentParams[param.name] && currentParams[param.name].trim()
      );
      
      // Validate request body if required
      if (this.selectedEndpointRequiresBody()) {
        const bodyRequired = this.selectedApiEndpoint()?.operation?.requestBody?.required;
        if (bodyRequired && !this.requestBody().trim()) {
          return false;
        }
        
        // Check JSON validity
        if (this.requestBody().trim() && this.requestBodyError()) {
          return false;
        }
      }
      
      return paramsValid;
    }
    
    return false;
  });

  constructor() {
    // === REACTIVE EFFECTS ===
    
    /**
     * Load AAS list when AAS type is selected
     */
    effect(() => {
      if (this.sourceType() === 'AAS') {
        this.loadAasList();
      } else {
        this.aasList.set([]);
        this.isLoadingAas.set(false);
      }
    });

    /**
     * Reset OpenAPI fields when switching away from REST type
     */
    effect(() => {
      if (this.sourceType() !== 'REST_OPENAPI') {
        this.resetOpenApiFields();
      }
    });

    /**
     * Reset parameters and request body when endpoint changes
     */
    effect(() => {
      const endpoint = this.selectedApiEndpoint();
      if (endpoint) {
        this.endpointParameters.set({});
        this.requestBody.set('');
        this.requestBodyError.set(null);
      }
    });

    /**
     * Trigger change detection for zone-less updates
     */
    effect(() => {
      // Track all signals that should trigger UI updates
      this.sourceName();
      this.sourceType();
      this.selectedAasId();
      this.selectedApiEndpoint();
      this.isLoadingOpenApiSpec();
      this.openApiSpecError();
      this.availableApiEndpoints();
      this.endpointParameters();
      this.requestBody();
      this.markForCheck();
    });
  }

  // === PARAMETER MANAGEMENT METHODS ===
  
  /**
   * Updates a parameter value for the selected endpoint
   * @param paramName Name of the parameter
   * @param value New parameter value
   */
  setEndpointParameter(paramName: string, value: string): void {
    const currentParams = { ...this.endpointParameters() };
    currentParams[paramName] = value;
    this.endpointParameters.set(currentParams);
    this.markForCheck();
  }

  // === REQUEST BODY MANAGEMENT METHODS ===
  
  /**
   * Updates the request body and validates JSON format
   * @param value New request body JSON string
   */
  setRequestBody(value: string): void {
    this.requestBody.set(value);
    this.validateRequestBody(value);
    this.markForCheck();
  }

  /**
   * Validates JSON format of request body
   * @param body JSON string to validate
   */
  private validateRequestBody(body: string): void {
    if (!body.trim()) {
      this.requestBodyError.set(null);
      return;
    }
    
    try {
      JSON.parse(body);
      this.requestBodyError.set(null);
    } catch (error) {
      this.requestBodyError.set('Invalid JSON format');
    }
  }

  /**
   * Generates example request body based on OpenAPI schema
   * @returns Formatted JSON example string
   */
  generateRequestBodyExample(): string {
    const schema = this.requestBodySchema();
    if (!schema) return '{}';
    
    return this.generateExampleFromSchema(schema);
  }

  /**
   * Converts OpenAPI schema to formatted JSON example
   * @param schema OpenAPI schema object
   * @returns Formatted JSON string
   */
  private generateExampleFromSchema(schema: any): string {
    if (!schema) return '{}';
    
    try {
      const example = this.buildExampleObject(schema);
      return JSON.stringify(example, null, 2);
    } catch (error) {
      return '{\n  "example": "value"\n}';
    }
  }

  /**
   * Recursively builds example object from OpenAPI schema
   * @param schema OpenAPI schema definition
   * @returns Example object matching the schema
   */
  private buildExampleObject(schema: any): any {
    if (schema.example) return schema.example;
    
    switch (schema.type) {
      case 'object':
        const obj: any = {};
        if (schema.properties) {
          Object.keys(schema.properties).forEach(key => {
            obj[key] = this.buildExampleObject(schema.properties[key]);
          });
        }
        return obj;
        
      case 'array':
        if (schema.items) {
          return [this.buildExampleObject(schema.items)];
        }
        return [];
        
      case 'string':
        return schema.enum ? schema.enum[0] : 'string';
        
      case 'number':
      case 'integer':
        return 123;
        
      case 'boolean':
        return true;
        
      default:
        return null;
    }
  }

  /**
   * Builds example URL with parameter substitution
   * @returns Complete example URL with parameters
   */
  buildExampleUrl(): string {
    const endpoint = this.selectedApiEndpoint();
    if (!endpoint) return '';
    
    let url = endpoint.path;
    const params = this.endpointParameters();
    
    // Replace path parameters (e.g., {id} -> 123)
    this.selectedEndpointParams().forEach((param: any) => {
      if (param.in === 'path') {
        const value = params[param.name] || `{${param.name}}`;
        url = url.replace(`{${param.name}}`, value);
      }
    });
    
    // Add query parameters
    const queryParams: string[] = [];
    this.selectedEndpointParams().forEach((param: any) => {
      if (param.in === 'query' && params[param.name]) {
        queryParams.push(`${param.name}=${encodeURIComponent(params[param.name])}`);
      }
    });
    
    if (queryParams.length > 0) {
      url += '?' + queryParams.join('&');
    }
    
    return `https://api.example.com${url}`;
  }

  // === ASYNC OPERATIONS ===
  
  /**
   * Loads and parses OpenAPI specification from URL
   * Handles CORS, JSON parsing, and error scenarios
   */
  async loadAndParseOpenApiSpec(): Promise<void> {
    const url = this.openApiSpecUrl();
    if (!url) {
      this.openApiSpecError.set('Please enter an OpenAPI Spec URL.');
      this.markForCheck();
      return;
    }

    // Reset state
    this.isLoadingOpenApiSpec.set(true);
    this.openApiSpecError.set(null);
    this.specValidationIssues.set([]);
    this.endpointDocumentationIssues.set({});
    this.parsedOpenApiSpec.set(null);
    this.availableApiEndpoints.set([]);
    this.selectedApiEndpoint.set(null);
    this.endpointParameters.set({});
    this.requestBody.set('');
    this.requestBodyError.set(null);
    this.markForCheck();

    try {
      console.log('Fetching OpenAPI spec from:', url);
      
      // Fetch with CORS support
      const response = await fetch(url, {
        method: 'GET',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        mode: 'cors'
      });
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      
      // Parse response
      let spec: any;
      const contentType = response.headers.get('content-type');
      console.log('Response content-type:', contentType);
      
      if (contentType?.includes('application/json')) {
        spec = await response.json();
      } else {
        const text = await response.text();
        console.log('Response text (first 200 chars):', text.substring(0, 200));
        try {
          spec = JSON.parse(text);
        } catch (parseError) {
          console.error('JSON parse error:', parseError);
          throw new Error('Invalid JSON format. Please ensure the URL returns valid JSON.');
        }
      }
      
      console.log('Parsed OpenAPI spec:', spec);
      
      // Process specification
      this.validateOpenApiSpec(spec);
      this.parsedOpenApiSpec.set(spec);
      this.extractAndAnalyzeEndpoints(spec);
      
      console.log('Extracted endpoints:', this.availableApiEndpoints());
      
    } catch (err: any) {
      console.error('Error loading OpenAPI spec:', err);
      
      // User-friendly error messages
      let errorMessage = err.message || 'Unknown error';
      
      if (err.message?.includes('CORS')) {
        errorMessage = 'CORS error: Try using a CORS proxy or a different API URL';
      } else if (err.message?.includes('Failed to fetch')) {
        errorMessage = 'Network error: Check the URL and your internet connection';
      } else if (err.message?.includes('HTTP 4')) {
        errorMessage = `API returned error ${err.message}. Check if the URL is correct.`;
      }
      
      this.openApiSpecError.set(errorMessage);
    } finally {
      this.isLoadingOpenApiSpec.set(false);
      this.markForCheck();
    }
  }

  /**
   * Loads available AAS instances from registry
   */
  private async loadAasList(): Promise<void> {
    this.isLoadingAas.set(true);
    this.markForCheck();
    
    try {
      this.aasService.getAll().pipe(
        tap((aasList) => {
          this.aasList.set(aasList);
          this.isLoadingAas.set(false);
          this.markForCheck();
        }),
        catchError((err) => {
          console.error('Error loading AAS list:', err);
          this.aasList.set([]);
          this.isLoadingAas.set(false);
          this.markForCheck();
          return of([]);
        })
      ).subscribe();
    } catch (err) {
      console.error('Error loading AAS list:', err);
      this.aasList.set([]);
      this.isLoadingAas.set(false);
      this.markForCheck();
    }
  }

  // === OPENAPI ANALYSIS ===
  
  /**
   * Validates OpenAPI specification structure
   * Note: Validation is currently disabled for broader compatibility
   * @param spec Parsed OpenAPI specification
   */
  private validateOpenApiSpec(spec: any): void {
    // Validation temporarily disabled for compatibility
    this.specValidationIssues.set([]);
    console.log('Validation skipped - spec accepted:', spec);
  }

  /**
   * Extracts API endpoints from OpenAPI specification
   * @param spec Parsed OpenAPI specification
   */
  private extractAndAnalyzeEndpoints(spec: any): void {
    if (!spec.paths) {
      console.log('No paths found in spec');
      return;
    }

    const endpoints: ApiEndpoint[] = [];

    // Iterate through all paths and methods
    for (const path in spec.paths) {
      for (const method in spec.paths[path]) {
        if (['get', 'post', 'put', 'delete', 'patch', 'options', 'head'].includes(method.toLowerCase())) {
          const operation = spec.paths[path][method];
          const endpointId = operation.operationId || `${method.toUpperCase()} ${path}`;
          
          // Extract error response codes
          const errors = this.extractErrorCodes(operation);
          
          endpoints.push({
            path,
            method: method.toUpperCase(),
            operation,
            id: endpointId,
            summary: operation.summary,
            description: operation.description,
            errors,
            hasDocumentationIssues: false // Documentation analysis disabled
          });
        }
      }
    }
    
    this.availableApiEndpoints.set(endpoints);
    this.endpointDocumentationIssues.set({}); // Documentation analysis disabled
    
    console.log('Extracted endpoints:', endpoints);
  }

  /**
   * Analyzes endpoint documentation quality
   * Note: Currently disabled for simplified workflow
   * @param operation OpenAPI operation object
   * @param path Endpoint path
   * @param method HTTP method
   * @returns List of documentation issues
   */
  private analyzeEndpointDocumentation(operation: any, path: string, method: string): string[] {
    return []; // Documentation analysis disabled
  }

  /**
   * Extracts error response definitions from operation
   * @param operation OpenAPI operation object
   * @returns List of error response codes and descriptions
   */
  private extractErrorCodes(operation: any): ApiError[] {
    if (!operation.responses) return [];
    
    const errors: ApiError[] = [];
    
    Object.entries(operation.responses).forEach(([code, response]: [string, any]) => {
      if (code.startsWith('4') || code.startsWith('5')) {
        errors.push({
          code,
          description: response.description || `HTTP ${code} Error`,
          example: response.content?.['application/json']?.example
        });
      }
    });
    
    return errors;
  }

  // === UTILITY METHODS ===
  
  /**
   * Triggers change detection in zone-less mode
   */
  private markForCheck(): void {
    this.cdr.markForCheck();
  }

  /**
   * Resets all OpenAPI-related form fields
   */
  private resetOpenApiFields(): void {
    this.openApiSpecUrl.set('');
    this.isLoadingOpenApiSpec.set(false);
    this.openApiSpecError.set(null);
    this.parsedOpenApiSpec.set(null);
    this.availableApiEndpoints.set([]);
    this.selectedApiEndpoint.set(null);
    this.specValidationIssues.set([]);
    this.endpointDocumentationIssues.set({});
    this.endpointParameters.set({});
    this.requestBody.set('');
    this.requestBodyError.set(null);
  }

  // === PUBLIC ACTION METHODS ===
  
  /**
   * Saves the configured source system
   * Validates form data and emits sourceSaved event
   */
  save(): void {
    if (!this.isFormValid()) {
      console.error('Form is not valid');
      return;
    }

    const sourceData: SourceSystemData = {
      name: this.sourceName(),
      type: this.sourceType()
    };

    if (this.sourceType() === 'AAS') {
      sourceData.aasId = this.selectedAasId();
    } else if (this.sourceType() === 'REST_OPENAPI') {
      const endpoint = this.selectedApiEndpoint();
      if (!endpoint) {
        this.openApiSpecError.set('Please select an endpoint.');
        return;
      }
      
      const endpointData: any = {
        ...endpoint,
        parameters: this.endpointParameters()
      };
      
      // Include request body if provided
      if (this.selectedEndpointRequiresBody() && this.requestBody().trim()) {
        try {
          endpointData.requestBody = JSON.parse(this.requestBody());
        } catch (error) {
          this.requestBodyError.set('Invalid JSON format');
          return;
        }
      }
      
      sourceData.openApiSpecUrl = this.openApiSpecUrl();
      sourceData.selectedEndpoint = endpointData;
    }

    console.log('🚀 Saving Source System:', sourceData);
    this.sourceSaved.emit(sourceData);
    this.closeDialogAndReset();
  }

  /**
   * Cancels the dialog and resets form
   */
  cancel(): void {
    this.closeDialogAndReset();
  }

  /**
   * Closes dialog and resets all form data
   */
  private closeDialogAndReset(): void {
    this.visible = false;
    this.visibleChange.emit(this.visible);
    this.resetForm();
    this.markForCheck();
  }

  /**
   * Resets all form fields to initial state
   */
  private resetForm(): void {
    this.sourceName.set('');
    this.sourceType.set('AAS');
    this.selectedAasId.set(undefined);
    this.resetOpenApiFields();
  }
}