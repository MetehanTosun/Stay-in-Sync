import {Component, EventEmitter, Input, OnInit, Output, OnDestroy} from '@angular/core';
import {CommonModule} from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';

import {TableModule} from 'primeng/table';
import {ButtonModule} from 'primeng/button';
import {InputTextModule} from 'primeng/inputtext';
import {DropdownModule} from 'primeng/dropdown';
import {CardModule} from 'primeng/card';
import {CheckboxModule} from 'primeng/checkbox';
import {DialogModule} from 'primeng/dialog';
import {ProgressSpinnerModule} from 'primeng/progressspinner';
import {TabViewModule} from 'primeng/tabview';
import { MonacoEditorModule, NgxEditorModel } from 'ngx-monaco-editor-v2';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { TooltipModule } from 'primeng/tooltip';

import {SourceSystemEndpointResourceService} from '../../service/sourceSystemEndpointResource.service';
import {HttpClient} from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import {SourceSystemResourceService} from '../../service/sourceSystemResource.service';
import {TypeScriptGenerationRequest} from '../../models/typescriptGenerationRequest';
import {TypeScriptGenerationResponse} from '../../models/typescriptGenerationResponse';

import {SourceSystemEndpointDTO} from '../../models/sourceSystemEndpointDTO';
import {ApiEndpointQueryParamType} from '../../models/apiEndpointQueryParamType';

import { load as parseYAML } from 'js-yaml';
import { SourceSystemDTO } from '../../models/sourceSystemDTO';
import { ManageEndpointParamsComponent } from '../manage-endpoint-params/manage-endpoint-params.component';
import { ResponsePreviewModalComponent } from '../response-preview-modal/response-preview-modal.component';
import { ConfirmationDialogComponent, ConfirmationDialogData } from '../confirmation-dialog/confirmation-dialog.component';
import { OpenApiImportService } from '../../../../core/services/openapi-import.service';
import { ManageEndpointsFormService } from '../../services/manage-endpoints-form.service';
import { TypeScriptGenerationService } from '../../services/typescript-generation.service';
import { ResponsePreviewService } from '../../services/response-preview.service';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import {JobStatusTagComponent} from '../../../../shared/components/job-status-tag/job-status-tag.component';
import {Select} from 'primeng/select';
import {FloatLabel} from 'primeng/floatlabel';
/**
 * Component for managing endpoints of a source system: list, create, edit, delete, and import.
 */
@Component({
  standalone: true,
  selector: 'app-manage-endpoints',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TableModule,
    ButtonModule,
    InputTextModule,
    DropdownModule,
    CardModule,
    CheckboxModule,
    DialogModule,
    ProgressSpinnerModule,
    TabViewModule,
    ManageEndpointParamsComponent,
    ResponsePreviewModalComponent,
    ConfirmationDialogComponent,
    MonacoEditorModule,
    DragDropModule,
    TooltipModule,
    Select,
    FloatLabel,
    ToastModule,
  ],
  templateUrl: './manage-endpoints.component.html',
  styleUrls: ['./manage-endpoints.component.css']
})
export class ManageEndpointsComponent implements OnInit, OnDestroy {
  public ApiEndpointQueryParamType = ApiEndpointQueryParamType;
  @Input() sourceSystemId!: number;
  @Output() backStep = new EventEmitter<void>();
  @Output() finish = new EventEmitter<void>();
  @Output() onCreated = new EventEmitter<void>();
  @Output() onDeleted = new EventEmitter<void>();
  @Output() onUpdated = new EventEmitter<void>();
  endpoints: SourceSystemEndpointDTO[] = [];
  endpointForm!: FormGroup;
  loading = false;
  selectedEndpoint: SourceSystemEndpointDTO | null = null;
  httpRequestTypes = [
    {label: 'GET', value: 'GET'},
    {label: 'POST', value: 'POST'},
    {label: 'PUT', value: 'PUT'},
    {label: 'DELETE', value: 'DELETE'}
  ];
  apiUrl: string | null = null;
  importing = false;
  editDialog: boolean = false;
  editingEndpoint: SourceSystemEndpointDTO | null = null;
  editForm!: FormGroup;
  jsonEditorOptions = {
    theme: 'vs-dark',
    language: 'json',
    automaticLayout: true,
    minimap: { enabled: false },
    scrollBeyondLastLine: false,
    wordWrap: 'on',
    folding: true,
    lineNumbers: 'on',
    formatOnPaste: true,
    formatOnType: true
  };

  typescriptEditorOptions = {
    theme: 'vs-dark',
    language: 'typescript',
    automaticLayout: true,
    minimap: { enabled: false },
    scrollBeyondLastLine: false,
    wordWrap: 'on',
    folding: true,
    lineNumbers: 'on',
    readOnly: true,
    // Enhanced TypeScript syntax highlighting options
    bracketPairColorization: { enabled: true },
    colorDecorators: true,
    contextmenu: false, // Disable context menu for readonly
    copyWithSyntaxHighlighting: true,
    cursorBlinking: 'solid',
    cursorStyle: 'line',
    fontSize: 13,
    fontFamily: 'Consolas, "Courier New", monospace',
    fontWeight: 'normal',
    letterSpacing: 0,
    lineHeight: 20,
    // TypeScript-specific options
    suggest: { enabled: false }, // Disable suggestions for readonly
    quickSuggestions: false, // Disable quick suggestions for readonly
    parameterHints: { enabled: false }, // Disable parameter hints for readonly
    hover: { enabled: true }, // Keep hover for type information
    // Code formatting options
    formatOnPaste: false,
    formatOnType: false,
    // Selection and navigation
    selectOnLineNumbers: true,
    roundedSelection: false,
    // Performance options
    renderWhitespace: 'none',
    renderControlCharacters: false,
    renderLineHighlight: 'all',
    // Performance optimizations
    largeFileOptimizations: true,
    maxTokenizationLineLength: 20000,
    maxTokenizationLineNumber: 1000,
    // Memory optimizations
    maxMemoryUsage: 512, // MB
    // Accessibility
    accessibilitySupport: 'auto'
  };
  jsonError: string | null = null;
  editJsonError: string | null = null;
  requestBodyEditorEndpoint: SourceSystemEndpointDTO | null = null;
  requestBodyEditorModel: NgxEditorModel = { value: '', language: 'json' };
  requestBodyEditorError: string | null = null;

  // Response Preview Modal properties
  responsePreviewModalVisible: boolean = false;
  selectedResponsePreviewEndpoint: SourceSystemEndpointDTO | null = null;

  // TypeScript generation properties
  generatedTypeScript: string = '';
  editGeneratedTypeScript: string = '';

  // Monaco Editor models for TypeScript
  typescriptModel: NgxEditorModel = { value: '', language: 'typescript' };
  editTypeScriptModel: NgxEditorModel = { value: '', language: 'typescript' };

  // Tab state management
  activeTabIndex: number = 0;
  editActiveTabIndex: number = 0;

  // Loading states for TypeScript generation
  isGeneratingTypeScript: boolean = false;
  isGeneratingEditTypeScript: boolean = false;

  // Error states for TypeScript generation
  typescriptError: string | null = null;
  editTypeScriptError: string | null = null;

  // Confirmation dialog properties
  showConfirmationDialog = false;
  confirmationData: ConfirmationDialogData = {
    title: 'Delete Endpoint',
    message: 'Are you sure you want to delete this endpoint? This action cannot be undone.',
    confirmLabel: 'Delete',
    cancelLabel: 'Cancel',
    severity: 'warning'
  };
  endpointToDelete: SourceSystemEndpointDTO | null = null;

  // Performance and timeout settings
  private readonly TYPESCRIPT_GENERATION_TIMEOUT = 30000; // 30 seconds
  private readonly MAX_JSON_SCHEMA_SIZE = 1024 * 1024; // 1MB
  private typescriptGenerationTimeout: any = null;
  private editTypeScriptGenerationTimeout: any = null;
  private lastToastTime: number = 0;
  private readonly TOAST_DEBOUNCE_TIME = 1000; // 1 second
  private toastCounter: number = 0;
  private activeToasts: Set<string> = new Set();

  constructor(
    private fb: FormBuilder,
    private endpointSvc: SourceSystemEndpointResourceService,
    private sourceSystemService: SourceSystemResourceService,
    private http: HttpClient,
    private openapi: OpenApiImportService,
    private formService: ManageEndpointsFormService,
    private typeScriptService: TypeScriptGenerationService,
    private responsePreviewService: ResponsePreviewService,
    private messageService: MessageService
  ) {}

  /**
   * Show toast message with debounce to prevent duplicates
   */
  private showToast(severity: 'success' | 'error' | 'info' | 'warn', summary: string, detail: string) {
    this.messageService.add({ key: 'endpoints', severity, summary, detail });
  }

  /**
   * Initialize forms and load endpoints and source system API URL.
   */
  ngOnInit(): void {
    this.endpointForm = this.formService.createEndpointForm();
    this.editForm = this.formService.createEditForm();

    // Subscribe to TypeScript generation states (keep advanced flow)
    this.typeScriptService.getMainGenerationState().subscribe(state => {
      this.isGeneratingTypeScript = state.isGenerating;
      this.generatedTypeScript = state.code;
      this.typescriptError = state.error;
      this.typescriptModel.value = state.code;
    });
    this.typeScriptService.getEditGenerationState().subscribe(state => {
      this.isGeneratingEditTypeScript = state.isGenerating;
      this.editGeneratedTypeScript = state.code;
      this.editTypeScriptError = state.error;
      this.editTypeScriptModel.value = state.code;
    });

    // Also trigger generation when schemas change
    this.endpointForm.get('responseBodySchema')?.valueChanges.subscribe(() => this.loadTypeScriptForMainForm());
    this.editForm.get('responseBodySchema')?.valueChanges.subscribe(() => this.loadTypeScriptForEditForm());

    // Reset tab indices when forms are reset
    this.endpointForm.valueChanges.subscribe(() => {
      // Keep track of form state for tab management
    });

    this.loadEndpoints()
    this.loadSourceSystemAndSetApiUrl();
  }

  /**
   * Clean up resources when component is destroyed
   */
  ngOnDestroy(): void {
    // Clear all timeouts
    this.clearTypeScriptGenerationTimeout(false);
    this.clearTypeScriptGenerationTimeout(true);
  }

  /**
   * Loads the source system and sets the API URL.
   */
  private loadSourceSystemAndSetApiUrl(): void {
    this.sourceSystemService.apiConfigSourceSystemIdGet(this.sourceSystemId)
      .subscribe({
        next: (sourceSystem: SourceSystemDTO) => {
          
          // Handle different types of openApiSpec
          if (sourceSystem.openApiSpec) {
            if (typeof sourceSystem.openApiSpec === 'string') {
              if (sourceSystem.openApiSpec.startsWith('http')) {
                this.apiUrl = sourceSystem.openApiSpec.trim();
                this.currentOpenApiSpec = '';
              } else {
                // Spec was provided as raw content (uploaded file). Keep a local copy to parse client-side.
                this.currentOpenApiSpec = sourceSystem.openApiSpec;
                this.apiUrl = sourceSystem.apiUrl!;
              }
            } else if (sourceSystem.openApiSpec && typeof sourceSystem.openApiSpec === 'object' && 'size' in sourceSystem.openApiSpec) {
              // Convert Blob to string
              const reader = new FileReader();
              reader.onload = () => {
                this.currentOpenApiSpec = reader.result as string;
                this.apiUrl = sourceSystem.apiUrl!;
              };
              reader.readAsText(sourceSystem.openApiSpec as any);
            } else {
              this.currentOpenApiSpec = '';
              this.apiUrl = sourceSystem.apiUrl!;
            }
          } else {
            this.currentOpenApiSpec = '';
            this.apiUrl = sourceSystem.apiUrl!;
          }
        },
        error: () => {
          this.apiUrl = null;
        }
      });
    
    // Load endpoints after source system is loaded
    this.loadEndpoints();
  }

  /**
   * Loads endpoints for the current source system from the backend.
   */
  loadEndpoints() {
    if (!this.sourceSystemId) return;
    this.loading = true;
    this.http.get<SourceSystemEndpointDTO[]>(`/api/config/source-system/${this.sourceSystemId}/endpoint`)
      .subscribe({
        next: (eps: SourceSystemEndpointDTO[]) => {
          this.endpoints = eps;
          this.loading = false;
          // Reset tab indices when endpoints are reloaded
          this.activeTabIndex = 0;
          this.editActiveTabIndex = 0;
          // Reset loading states
          this.isGeneratingTypeScript = false;
          this.isGeneratingEditTypeScript = false;
          // Reset error states
          this.typescriptError = null;
          this.editTypeScriptError = null;
          // Clear timeouts
          this.clearTypeScriptGenerationTimeout(false);
          this.clearTypeScriptGenerationTimeout(true);
        },
        error: (error) => {
          console.error('[ManageEndpoints] Error loading endpoints:', error);
          this.loading = false;
        }
      });
  }

  /**
   * Load TypeScript data for the main form with comprehensive error handling
   */
  loadTypeScriptForMainForm() {
    const responseBodySchema = this.endpointForm.get('responseBodySchema')?.value;
    this.typescriptError = null; // Clear previous errors
    this.clearTypeScriptGenerationTimeout(false);

    if (responseBodySchema) {
      // Validate JSON schema first
      const validation = this.validateJsonSchema(responseBodySchema);
      if (!validation.isValid) {
        this.typescriptError = validation.error || 'Invalid JSON schema';
        this.generatedTypeScript = this.getTypeScriptErrorFallback(responseBodySchema);
        return;
      }

      this.isGeneratingTypeScript = true;
      this.generatedTypeScript = '';

      // Set timeout for generation
      this.handleTypeScriptGenerationTimeout(false);

      // Use backend service for TypeScript generation
      const request: TypeScriptGenerationRequest = {
        jsonSchema: responseBodySchema
      };

      // For now, use a temporary endpoint ID (0) since we don't have a real endpoint yet
      // In the future, this should use the actual endpoint ID when editing existing endpoints
      this.endpointSvc.generateTypeScript(0, request).subscribe({
        next: (response: TypeScriptGenerationResponse) => {
          this.clearTypeScriptGenerationTimeout(false);

          if (response.error) {
            this.typescriptError = this.formatErrorMessage(response.error, 'Backend generation failed');
            this.generatedTypeScript = this.getTypeScriptErrorFallback(responseBodySchema);
          } else if (response.generatedTypeScript) {
            this.generatedTypeScript = response.generatedTypeScript;
            this.typescriptModel = { value: this.generatedTypeScript, language: 'typescript' };
          } else {
            this.typescriptError = 'No TypeScript generated from backend';
            this.generatedTypeScript = this.getTypeScriptErrorFallback(responseBodySchema);
            this.typescriptModel = { value: this.generatedTypeScript, language: 'typescript' };
          }
        },
        error: (error) => {
          this.clearTypeScriptGenerationTimeout(false);
          this.typescriptError = this.formatErrorMessage(error.message || 'Unknown error', 'Backend communication failed');
          this.generatedTypeScript = this.getTypeScriptErrorFallback(responseBodySchema);
          this.typescriptModel = { value: this.generatedTypeScript, language: 'typescript' };
        },
        complete: () => {
          this.clearTypeScriptGenerationTimeout(false);
          this.isGeneratingTypeScript = false;
        }
      });
    } else {
      this.generatedTypeScript = '';
      this.typescriptModel = { value: '', language: 'typescript' };
      this.isGeneratingTypeScript = false;
    }
  }

  /**
   * Load TypeScript data for the edit form with comprehensive error handling
   */
  loadTypeScriptForEditForm() {
    const responseBodySchema = this.editForm.get('responseBodySchema')?.value;
    this.editTypeScriptError = null; // Clear previous errors
    this.clearTypeScriptGenerationTimeout(true);

    if (responseBodySchema) {
      // Validate JSON schema first
      const validation = this.validateJsonSchema(responseBodySchema);
      if (!validation.isValid) {
        this.editTypeScriptError = validation.error || 'Invalid JSON schema';
        this.editGeneratedTypeScript = this.getTypeScriptErrorFallback(responseBodySchema);
        return;
      }

      this.isGeneratingEditTypeScript = true;
      this.editGeneratedTypeScript = '';

      // Set timeout for generation
      this.handleTypeScriptGenerationTimeout(true);

      // Use backend service for TypeScript generation
      const request: TypeScriptGenerationRequest = {
        jsonSchema: responseBodySchema
      };

      // Use the actual endpoint ID if available, otherwise use 0
      const endpointId = this.editingEndpoint?.id || 0;

      this.endpointSvc.generateTypeScript(endpointId, request).subscribe({
        next: (response: TypeScriptGenerationResponse) => {
          this.clearTypeScriptGenerationTimeout(true);

          if (response.error) {
            this.editTypeScriptError = this.formatErrorMessage(response.error, 'Backend generation failed');
            this.editGeneratedTypeScript = this.getTypeScriptErrorFallback(responseBodySchema);
          } else if (response.generatedTypeScript) {
            this.editGeneratedTypeScript = response.generatedTypeScript;
            this.editTypeScriptModel = { value: this.editGeneratedTypeScript, language: 'typescript' };
          } else {
            this.editTypeScriptError = 'No TypeScript generated from backend';
            this.editGeneratedTypeScript = this.getTypeScriptErrorFallback(responseBodySchema);
            this.editTypeScriptModel = { value: this.editGeneratedTypeScript, language: 'typescript' };
          }
        },
        error: (error) => {
          this.clearTypeScriptGenerationTimeout(true);
          this.editTypeScriptError = this.formatErrorMessage(error.message || 'Unknown error', 'Backend communication failed');
          this.editGeneratedTypeScript = this.getTypeScriptErrorFallback(responseBodySchema);
          this.editTypeScriptModel = { value: this.editGeneratedTypeScript, language: 'typescript' };
        },
        complete: () => {
          this.clearTypeScriptGenerationTimeout(true);
          this.isGeneratingEditTypeScript = false;
        }
      });
    } else {
      this.editGeneratedTypeScript = '';
      this.editTypeScriptModel = { value: '', language: 'typescript' };
      this.isGeneratingEditTypeScript = false;
    }
  }

  /**
   * Load TypeScript data from backend responseDts (when available)
   */
  loadTypeScriptFromBackend(endpoint: SourceSystemEndpointDTO) {
    // This method will be used when the backend provides responseDts
    // For now, it's a placeholder for future implementation
    if (endpoint.responseDts) {
      return endpoint.responseDts;
    }
    return null;
  }

  /**
   * Comprehensive JSON validation with size and format checks
   */
  private validateJsonSchema(jsonSchema: string): { isValid: boolean; error?: string } {
    // Check if schema is empty
    if (!jsonSchema || jsonSchema.trim().length === 0) {
      return { isValid: false, error: 'JSON schema is empty' };
    }

    // Check schema size
    if (jsonSchema.length > this.MAX_JSON_SCHEMA_SIZE) {
      return {
        isValid: false,
        error: `JSON schema is too large (${(jsonSchema.length / 1024).toFixed(1)}KB). Maximum size is ${(this.MAX_JSON_SCHEMA_SIZE / 1024).toFixed(0)}KB.`
      };
    }

    // Validate JSON syntax
    try {
      const parsed = JSON.parse(jsonSchema);

      // Check if it's a valid JSON Schema structure
      if (typeof parsed !== 'object' || parsed === null) {
        return { isValid: false, error: 'JSON schema must be an object' };
      }

      // Basic JSON Schema validation
      if (!this.isValidJsonSchemaStructure(parsed)) {
        return { isValid: false, error: 'Invalid JSON Schema structure. Expected properties like "type", "properties", or "$schema"' };
      }

      return { isValid: true };
    } catch (e) {
      return { isValid: false, error: `Invalid JSON syntax: ${e instanceof Error ? e.message : 'Unknown error'}` };
    }
  }

  /**
   * Check if parsed JSON has valid JSON Schema structure
   */
  private isValidJsonSchemaStructure(schema: any): boolean {
    // Check for common JSON Schema properties
    const hasType = schema.hasOwnProperty('type');
    const hasProperties = schema.hasOwnProperty('properties');
    const hasSchema = schema.hasOwnProperty('$schema');
    const hasRef = schema.hasOwnProperty('$ref');
    const hasItems = schema.hasOwnProperty('items');
    const hasEnum = schema.hasOwnProperty('enum');

    // Must have at least one of these properties to be a valid schema
    return hasType || hasProperties || hasSchema || hasRef || hasItems || hasEnum;
  }

  /**
   * Legacy method for backward compatibility
   */
  private isValidJson(str: string): boolean {
    return this.validateJsonSchema(str).isValid;
  }

  /**
   * Handle timeout for TypeScript generation
   */
  private handleTypeScriptGenerationTimeout(isEditForm: boolean = false): void {
    const timeoutId = isEditForm ? this.editTypeScriptGenerationTimeout : this.typescriptGenerationTimeout;

    if (timeoutId) {
      clearTimeout(timeoutId);
    }

    const newTimeoutId = setTimeout(() => {
      if (isEditForm) {
        this.editTypeScriptError = 'TypeScript generation timed out. Please try again or check your JSON schema.';
        this.isGeneratingEditTypeScript = false;
      } else {
        this.typescriptError = 'TypeScript generation timed out. Please try again or check your JSON schema.';
        this.isGeneratingTypeScript = false;
      }
    }, this.TYPESCRIPT_GENERATION_TIMEOUT);

    if (isEditForm) {
      this.editTypeScriptGenerationTimeout = newTimeoutId;
    } else {
      this.typescriptGenerationTimeout = newTimeoutId;
    }
  }

  /**
   * Clear timeout for TypeScript generation
   */
  private clearTypeScriptGenerationTimeout(isEditForm: boolean = false): void {
    const timeoutId = isEditForm ? this.editTypeScriptGenerationTimeout : this.typescriptGenerationTimeout;

    if (timeoutId) {
      clearTimeout(timeoutId);
      if (isEditForm) {
        this.editTypeScriptGenerationTimeout = null;
      } else {
        this.typescriptGenerationTimeout = null;
      }
    }
  }

  /**
   * Format error messages for better user experience
   */
  private formatErrorMessage(error: string, context: string): string {
    // Remove technical details and make error more user-friendly
    let formattedError = error;

    // Common error patterns
    if (error.includes('HttpErrorResponse')) {
      formattedError = 'Network error: Unable to connect to the server';
    } else if (error.includes('timeout')) {
      formattedError = 'Request timed out. Please try again.';
    } else if (error.includes('JSON')) {
      formattedError = 'Invalid JSON format in schema';
    } else if (error.includes('schema')) {
      formattedError = 'Invalid schema structure';
    } else if (error.includes('size') || error.includes('large')) {
      formattedError = 'Schema is too large to process';
    }

    return `${context}: ${formattedError}`;
  }

  /**
   * Get TypeScript error fallback content
   */
  private getTypeScriptErrorFallback(jsonSchema: string): string {
    return `// Error: Unable to generate TypeScript interface
// Please check your JSON Schema format

// Original JSON Schema:
${jsonSchema}

// Common issues:
// - Invalid JSON syntax
// - Missing required schema properties
// - Unsupported schema types
// - Circular references

// Please fix the JSON Schema and try again.`;
  }

  /**
   * Generate a placeholder TypeScript interface from JSON schema
   * This is a temporary implementation until we get the real responseDts from backend
   */
  private generateTypeScriptPlaceholder(jsonSchema: string): string {
    try {
      const schema = JSON.parse(jsonSchema);
      return this.convertJsonSchemaToTypeScript(schema);
    } catch (e) {
      return '// Invalid JSON Schema\n// Unable to generate TypeScript interface';
    }
  }

  /**
   * Convert JSON Schema to TypeScript interface
   */
  private convertJsonSchemaToTypeScript(schema: any): string {
    if (!schema || typeof schema !== 'object') {
      return '// Invalid schema';
    }

    let result = '// Generated TypeScript interface from JSON Schema\n';
    result += '// Auto-generated by Stay-in-Sync Configurator\n\n';

    if (schema.type === 'object' && schema.properties) {
      result += '/**\n';
      result += ' * Response body interface generated from JSON Schema\n';
      if (schema.description) {
        result += ` * ${schema.description}\n`;
      }
      result += ' */\n';
      result += 'interface ResponseBody {\n';

      for (const [key, prop] of Object.entries(schema.properties)) {
        const propSchema = prop as any;
        const type = this.getTypeScriptType(propSchema);
        const required = schema.required?.includes(key) ? '' : '?';

        // Add JSDoc comment for the property
        if (propSchema.description) {
          result += `  /** ${propSchema.description} */\n`;
        }

        result += `  ${key}${required}: ${type};\n`;
      }
      result += '}\n\n';
      result += 'export default ResponseBody;';
    } else if (schema.type === 'array' && schema.items) {
      result += '/**\n';
      result += ' * Response body type for array data\n';
      if (schema.description) {
        result += ` * ${schema.description}\n`;
      }
      result += ' */\n';
      result += 'type ResponseBody = ';
      result += this.getTypeScriptType(schema.items);
      result += '[];\n\n';
      result += 'export default ResponseBody;';
    } else {
      result += '/**\n';
      result += ' * Response body type\n';
      if (schema.description) {
        result += ` * ${schema.description}\n`;
      }
      result += ' */\n';
      result += 'type ResponseBody = ';
      result += this.getTypeScriptType(schema);
      result += ';\n\n';
      result += 'export default ResponseBody;';
    }

    return result;
  }

  /**
   * Get TypeScript type from JSON Schema type
   */
  private getTypeScriptType(schema: any): string {
    if (!schema || typeof schema !== 'object') {
      return 'any';
    }

    const type = schema.type;

    switch (type) {
      case 'string':
        // Handle string enums
        if (schema.enum && Array.isArray(schema.enum)) {
          const enumValues = schema.enum.map((v: string) => `'${v}'`).join(' | ');
          return enumValues;
        }
        // Handle string format
        if (schema.format === 'date-time') {
          return 'string'; // Could be 'Date' but string is more flexible
        }
        if (schema.format === 'email') {
          return 'string';
        }
        if (schema.format === 'uri') {
          return 'string';
        }
        return 'string';

      case 'number':
      case 'integer':
        // Handle number enums
        if (schema.enum && Array.isArray(schema.enum)) {
          const enumValues = schema.enum.join(' | ');
          return enumValues;
        }
        return 'number';

      case 'boolean':
        return 'boolean';

      case 'array':
        if (schema.items) {
          const itemType = this.getTypeScriptType(schema.items);
          return `${itemType}[]`;
        }
        return 'any[]';

      case 'object':
        if (schema.properties) {
          let result = '{\n';
          for (const [key, prop] of Object.entries(schema.properties)) {
            const propSchema = prop as any;
            const propType = this.getTypeScriptType(propSchema);
            const required = schema.required?.includes(key) ? '' : '?';
            result += `    ${key}${required}: ${propType};\n`;
          }
          result += '  }';
          return result;
        }
        return 'object';

      case 'null':
        return 'null';

      default:
        // Handle $ref or other complex types
        if (schema.$ref) {
          return 'any'; // Could be enhanced to resolve references
        }
        return 'any';
    }
  }

  /**
   * Create a new endpoint using form data and refresh list upon success.
   */
  addEndpoint() {
    this.addEndpointVisible = false;
    let requestBodySchema = this.endpointForm.get('requestBodySchema')?.value || '';
    let resolvedSchema = requestBodySchema;
    try {
      const parsed = JSON.parse(requestBodySchema);
      if (parsed && parsed.$ref && this.currentOpenApiSpec) {
        const openApi = typeof this.currentOpenApiSpec === 'string'
          ? JSON.parse(this.currentOpenApiSpec)
          : this.currentOpenApiSpec;
        const schemas = openApi.components?.schemas || {};
        const resolved = this.resolveRefs(parsed, schemas);
        resolvedSchema = JSON.stringify(resolved, null, 2);
      }
    } catch {
      // Kein JSON, lasse wie es ist
    }

    // Validate response body schema
    let responseBodySchema = this.endpointForm.get('responseBodySchema')?.value || '';
    if (responseBodySchema) {
      try {
        JSON.parse(responseBodySchema);
      } catch (e) {
        this.jsonError = 'Response-Body-Schema ist kein valides JSON.';
        return;
      }
    }

    const dto: SourceSystemEndpointDTO = {
      sourceSystemId: this.sourceSystemId!,
      endpointPath: this.endpointForm.get('endpointPath')?.value,
      httpRequestType: this.endpointForm.get('httpRequestType')?.value,
      requestBodySchema: resolvedSchema,
      responseBodySchema: responseBodySchema
    };
    this.http.post<SourceSystemEndpointDTO[]>(`/api/config/source-system/${this.sourceSystemId}/endpoint`, [dto])
      .subscribe({
      next: () => {
        this.loadEndpoints();
        this.endpointForm.reset({
          endpointPath: '',
          httpRequestType: 'GET',
          requestBodySchema: '',
          responseBodySchema: ''
        });
        // Clear TypeScript when form is reset
        this.generatedTypeScript = '';
        this.activeTabIndex = 0; // Reset to JSON tab
        this.isGeneratingTypeScript = false; // Reset loading state
        this.typescriptError = null; // Reset error state

        // Clear timeouts
        this.clearTypeScriptGenerationTimeout(false);

        // Ensure proper tab integration after form reset
        this.resetTabIntegration();
        this.onCreated.emit();
        this.showToast('success','Endpoint Created','Endpoint has been successfully created.');
      },
      error: (error) => {
        console.error('[ManageEndpoints] Error creating endpoint:', error);
      }
    });
  }

  /**
   * Show confirmation dialog for deleting an endpoint.
   */
  deleteEndpoint(endpoint: SourceSystemEndpointDTO) {
    this.endpointToDelete = endpoint;
    this.confirmationData = {
      title: 'Delete Endpoint',
      message: `Are you sure you want to delete the endpoint "${endpoint.endpointPath}" (${endpoint.httpRequestType})? This action cannot be undone and will also delete all associated query parameters.`,
      confirmLabel: 'Delete',
      cancelLabel: 'Cancel',
      severity: 'warning'
    };
    this.showConfirmationDialog = true;
  }

  /**
   * Handle confirmation dialog events.
   */
  onConfirmationConfirmed(): void {
    if (this.endpointToDelete && this.endpointToDelete.id) {
      this.http.delete(`/api/config/source-system/endpoint/${this.endpointToDelete.id}`)
        .subscribe({
          next: () => {
            this.endpoints = this.endpoints.filter(e => e.id !== this.endpointToDelete!.id);
            this.endpointToDelete = null;
            this.onDeleted.emit();
            this.showToast('success','Endpoint Deleted','Endpoint has been successfully deleted.');
          },
          error: (error) => {
            console.error('Error deleting endpoint:', error);
            this.endpointToDelete = null;
          }
        });
    }
  }

  onConfirmationCancelled(): void {
    this.endpointToDelete = null;
  }

  /**
   * Open the edit dialog pre-filled with endpoint data.
   */
  openEditDialog(endpoint: SourceSystemEndpointDTO) {
    this.editingEndpoint = endpoint;
    this.editForm.patchValue({
      endpointPath: endpoint.endpointPath,
      httpRequestType: endpoint.httpRequestType,
      requestBodySchema: endpoint.requestBodySchema || '',
      responseBodySchema: endpoint.responseBodySchema || ''
    });
    this.editDialog = true;
    this.editJsonError = null;
    this.editActiveTabIndex = 0; // Reset to JSON tab

    // Load TypeScript for the edit form
    this.loadTypeScriptForEditForm();

    // Ensure proper tab integration
    this.initializeTabIntegration();
  }

  /**
   * Initialize tab integration and ensure proper state
   */
  private initializeTabIntegration() {
    // Small delay to ensure DOM is ready
    setTimeout(() => {
      // Trigger change detection for tab state
      this.editActiveTabIndex = this.editActiveTabIndex;
    }, 100);
  }

  /**
   * Close the edit dialog and clear TypeScript data.
   */
  closeEditDialog() {
    this.editDialog = false;
    this.editGeneratedTypeScript = '';
    this.editTypeScriptModel = { value: '', language: 'typescript' };
    this.editJsonError = null;
    this.editActiveTabIndex = 0; // Reset to JSON tab
    this.isGeneratingEditTypeScript = false; // Reset loading state
    this.editTypeScriptError = null; // Reset error state

    // Clear timeouts
    this.clearTypeScriptGenerationTimeout(true);
    this.clearTypeScriptGenerationTimeout(false);

    // Clean up tab integration
    this.cleanupTabIntegration();
  }

  /**
   * Clean up tab integration state
   */
  private cleanupTabIntegration() {
    // Reset any tab-specific state
    this.editActiveTabIndex = 0;
    this.activeTabIndex = 0;
  }

  /**
   * Reset tab integration after form reset
   */
  private resetTabIntegration() {
    // Ensure tab state is properly reset
    this.activeTabIndex = 0;
    this.generatedTypeScript = '';
    this.typescriptModel = { value: '', language: 'typescript' };
    this.typescriptError = null;
    this.isGeneratingTypeScript = false;
  }

  /**
   * Handle tab change in main form
   */
  onTabChange(event: any) {
    this.activeTabIndex = event.index;
    // If switching to TypeScript tab and no TypeScript is generated yet, generate it
    if (event.index === 1 && !this.generatedTypeScript && !this.isGeneratingTypeScript) {
      this.loadTypeScriptForMainForm();
    }
  }

  /**
   * Handle tab change in edit form
   */
  onEditTabChange(event: any) {
    this.editActiveTabIndex = event.index;
    // If switching to TypeScript tab and no TypeScript is generated yet, generate it
    if (event.index === 1 && !this.editGeneratedTypeScript && !this.isGeneratingEditTypeScript) {
      this.loadTypeScriptForEditForm();
    }
  }

  /**
   * Save changes made to the editing endpoint and refresh list.
   */
  saveEdit() {
    if (!this.editingEndpoint || this.editForm.invalid) {
      return;
    }
    this.editJsonError = null;
    const dto: SourceSystemEndpointDTO = {
      id: this.editingEndpoint.id,
      sourceSystemId: this.sourceSystemId!,
      endpointPath: this.editForm.value.endpointPath,
      httpRequestType: this.editForm.value.httpRequestType,
      requestBodySchema: this.editForm.value.requestBodySchema,
      responseBodySchema: this.editForm.value.responseBodySchema,
      responseDts: this.editingEndpoint.responseDts
    };
    if (dto.requestBodySchema) {
      try {
        JSON.parse(dto.requestBodySchema);
      } catch (e) {
        this.editJsonError = 'Request-Body-Schema ist kein valides JSON.';
        return;
      }
    }
    if (dto.responseBodySchema) {
      try {
        JSON.parse(dto.responseBodySchema);
      } catch (e) {
        this.editJsonError = 'Response-Body-Schema ist kein valides JSON.';
        return;
      }
    }
    // Use direct HTTP call like Target System
    this.http.put(`/api/config/source-system/endpoint/${this.editingEndpoint.id}`, dto)
      .subscribe({
        next: (response) => {
          this.closeEditDialog();
          this.loadEndpoints();
          this.onUpdated.emit();
          this.showToast('success','Endpoint Updated','Endpoint has been successfully updated.');
        },
        error: (error) => {
          console.error('Error updating endpoint:', error);
          let errorMessage = 'Failed to update endpoint';
          if (error.status === 500) {
            errorMessage = 'Server error (500): Please check the backend logs';
          } else if (error.status === 404) {
            errorMessage = 'Endpoint not found (404)';
          } else if (error.status === 400) {
            errorMessage = 'Bad request (400): Please check your input data';
          }
          // Error handling - no toast message needed
        }
      });
  }

  /**
   * Show the request body editor for a given endpoint.
   */
  showRequestBodyEditor(endpoint: SourceSystemEndpointDTO) {
    
    this.requestBodyEditorEndpoint = endpoint;
    if (!endpoint.requestBodySchema) {
      this.requestBodyEditorModel = {
        value: JSON.stringify({
          error: 'No request body schema defined for this endpoint',
          endpoint: endpoint.endpointPath,
          method: endpoint.httpRequestType,
          note: 'This endpoint does not require a request body or no schema is defined in the OpenAPI specification.'
        }, null, 2),
        language: 'json'
      };
      return;
    }
    const resolved = this.resolveSchemaReference(endpoint.requestBodySchema);
    this.requestBodyEditorModel = {
      value: resolved,
      language: 'json'
    };
  }

  /**
   * Show the response preview modal for a given endpoint.
   */
  showResponsePreviewModal(endpoint: SourceSystemEndpointDTO) {
    try {
      // Validate endpoint data
      if (!endpoint) {
        return;
      }

      if (!endpoint.id) {
        return;
      }

      // Set the selected endpoint and open modal
      this.selectedResponsePreviewEndpoint = endpoint;
      this.responsePreviewModalVisible = true;
    } catch (error) {
      // In a real application, you might want to show a toast notification here
    }
  }

  /**
   * Close the response preview modal and clean up data.
   */
  closeResponsePreviewModal() {
    this.responsePreviewModalVisible = false;
    this.selectedResponsePreviewEndpoint = null;
  }

  /**
   * Handle response preview modal visibility changes.
   */
  onResponsePreviewModalVisibleChange(visible: boolean) {
    if (!visible) {
      // Modal is being closed, clean up data
      this.closeResponsePreviewModal();
    }
  }

  /**
   * Cleans a JSON schema by removing null values and unnecessary properties and resolving schema references.
   */
  private cleanJsonSchema(schema: any): any {
    if (!schema || typeof schema !== 'object') {
      return schema;
    }
    if (schema.$ref) {
      const resolvedSchemaStr = this.resolveSchemaReference(schema);
      try {
        const resolvedSchema = JSON.parse(resolvedSchemaStr);
        return this.cleanJsonSchema(resolvedSchema);
      } catch {
        return schema;
      }
    }
    const cleaned: any = {};
    const relevantProps = [
      'type', 'properties', 'required', 'items', 'allOf', 'anyOf', 'oneOf',
      'not', 'additionalProperties', 'description', 'format', '$ref',
      'nullable', 'readOnly', 'writeOnly', 'example', 'deprecated',
      'xml', 'discriminator', 'enum', 'const', 'default', 'minItems',
      'maxItems', 'uniqueItems', 'minProperties', 'maxProperties',
      'minLength', 'maxLength', 'pattern', 'minimum', 'maximum',
      'exclusiveMinimum', 'exclusiveMaximum', 'multipleOf'
    ];
    for (const prop of relevantProps) {
      if (schema[prop] !== null && schema[prop] !== undefined) {
        if (typeof schema[prop] === 'object' && !Array.isArray(schema[prop])) {
          cleaned[prop] = this.cleanJsonSchema(schema[prop]);
        } else if (Array.isArray(schema[prop])) {
          cleaned[prop] = schema[prop].map((item: any) =>
            typeof item === 'object' ? this.cleanJsonSchema(item) : item
          );
        } else {
          cleaned[prop] = schema[prop];
        }
      }
    }
    return cleaned;
  }

  /**
   * Resolves a schema reference ($ref) in the OpenAPI spec and returns the resolved schema as a string.
   */
  private resolveSchemaReference(schemaInput: any): string {
    
    try {
      let schemaObj = typeof schemaInput === 'string' ? JSON.parse(schemaInput) : schemaInput;
      
      if (schemaObj && schemaObj.$ref && this.currentOpenApiSpec) {
        const openApi = typeof this.currentOpenApiSpec === 'string'
          ? JSON.parse(this.currentOpenApiSpec)
          : this.currentOpenApiSpec;
        
        if (schemaObj.$ref.startsWith('#/components/schemas/')) {
          const schemaName = schemaObj.$ref.replace('#/components/schemas/', '');
          const resolved = openApi.components?.schemas?.[schemaName];
          
          if (resolved) {
            if (resolved.$ref) {
              return this.resolveSchemaReference(resolved);
            }
            return JSON.stringify(resolved, null, 2);
          }
        }
      }
    } catch (e) {
    }
    return typeof schemaInput === 'string' ? schemaInput : JSON.stringify(schemaInput, null, 2);
  }

  /**
   * Parses the OpenAPI spec (JSON or YAML) and resolves the schema reference.
   */
  private parseAndResolveSchema(specText: string | undefined, ref: string): void {
    if (!specText) {
      this.requestBodyEditorModel = {
        value: JSON.stringify({ $ref: ref, error: 'No OpenAPI spec available' }, null, 2),
        language: 'json'
      };
      return;
    }
    try {
      let spec: any;
      try {
        spec = JSON.parse(specText);
      } catch (jsonError) {
        try {
          spec = this.parseYamlToJson(specText);
        } catch (yamlError) {
          throw new Error('Failed to parse OpenAPI spec as JSON or YAML');
        }
      }
      const schemas = spec.components?.schemas || {};
      const schemaName = ref.replace('#/components/schemas/', '');
      const schema = schemas[schemaName];
      if (schema) {
        const resolvedSchema = this.resolveRefs(schema, schemas);
        this.requestBodyEditorModel = {
          value: JSON.stringify(resolvedSchema, null, 2),
          language: 'json'
        };
      } else {
        this.requestBodyEditorModel = {
          value: JSON.stringify({ $ref: ref, error: 'Schema not found in OpenAPI spec' }, null, 2),
          language: 'json'
        };
      }
    } catch (e) {
      this.requestBodyEditorModel = {
        value: JSON.stringify({ $ref: ref, error: 'Failed to parse OpenAPI spec' }, null, 2),
        language: 'json'
      };
    }
  }

  /**
   * Converts YAML to JSON for basic OpenAPI specs.
   */
  private parseYamlToJson(yamlText: string): any {
    try {
      const cleanedYaml = this.cleanYaml(yamlText);
      const result = this.simpleYamlParse(cleanedYaml);
      return result;
    } catch (e) {
      throw new Error('YAML parsing failed');
    }
  }

  /**
   * Removes comments and unnecessary characters from YAML text.
   */
  private cleanYaml(yamlText: string): string {
    const lines = yamlText.split('\n');
    const cleanedLines = lines.map(line => {
      const commentIndex = line.indexOf('#');
      if (commentIndex >= 0) {
        line = line.substring(0, commentIndex);
      }
      return line;
    });
    return cleanedLines.join('\n');
  }

  /**
   * Simple YAML parsing implementation for OpenAPI specs.
   */
  private simpleYamlParse(yamlText: string): any {
    const lines = yamlText.split('\n').filter(line => line.trim() !== '');
    const result: any = {};
    const path: string[] = [];
    const stack: any[] = [result];
    for (const line of lines) {
      const indent = this.getIndentLevel(line);
      const content = line.trim();
      if (content === '') continue;
      while (path.length > indent / 2) {
        path.pop();
        stack.pop();
      }
      if (content.includes(':')) {
        const colonIndex = content.indexOf(':');
        const key = content.substring(0, colonIndex).trim();
        const value = content.substring(colonIndex + 1).trim();
        if (value === '') {
          const newObj: any = {};
          this.setNestedValue(result, [...path, key], newObj);
          path.push(key);
          stack.push(newObj);
        } else {
          this.setNestedValue(result, [...path, key], this.parseValue(value));
        }
      } else if (content.startsWith('- ')) {
        const value = content.substring(2);
        const currentPath = [...path];
        const parentKey = currentPath.pop();
        if (parentKey) {
          const parent = this.getNestedValue(result, currentPath);
          if (!Array.isArray(parent[parentKey])) {
            parent[parentKey] = [];
          }
          parent[parentKey].push(this.parseValue(value));
        }
      }
    }
    return result;
  }

  /**
   * Sets a value in a nested object.
   */
  private setNestedValue(obj: any, path: string[], value: any): void {
    let current = obj;
    for (let i = 0; i < path.length - 1; i++) {
      if (!current[path[i]]) {
        current[path[i]] = {};
      }
      current = current[path[i]];
    }
    current[path[path.length - 1]] = value;
  }

  /**
   * Gets a value from a nested object.
   */
  private getNestedValue(obj: any, path: string[]): any {
    let current = obj;
    for (const key of path) {
      if (current && typeof current === 'object' && key in current) {
        current = current[key];
      } else {
        return undefined;
      }
    }
    return current;
  }

  /**
   * Determines the indentation level of a line.
   */
  private getIndentLevel(line: string): number {
    let level = 0;
    for (let i = 0; i < line.length; i++) {
      if (line[i] === ' ' || line[i] === '\t') {
        level++;
      } else {
        break;
      }
    }
    return level;
  }

  /**
   * Parses a single YAML value.
   */
  private parseValue(value: string): any {
    value = value.trim();
    if (value === 'true') return true;
    if (value === 'false') return false;
    if (value === 'null' || value === '') return null;
    if (!isNaN(Number(value))) {
      return Number(value);
    }
    if ((value.startsWith('"') && value.endsWith('"')) ||
        (value.startsWith("'") && value.endsWith("'"))) {
      return value.slice(1, -1);
    }
    return value;
  }

  /**
   * Closes the request body editor.
   */
  closeRequestBodyEditor() {
    this.requestBodyEditorEndpoint = null;
    this.requestBodyEditorModel = { value: '', language: 'json' };
    this.requestBodyEditorError = null;
  }

  /**
   * Saves the request body schema from the editor.
   */
  saveRequestBodySchema() {
    if (!this.requestBodyEditorEndpoint) return;
    let schemaValue = this.requestBodyEditorModel.value;
    schemaValue = this.resolveSchemaReference(schemaValue);
    this.requestBodyEditorEndpoint.requestBodySchema = schemaValue;
    this.closeRequestBodyEditor();
  }

  /**
   * Handles changes in the request body editor model.
   */
  onRequestBodyModelChange(event: any) {
    let value: string = '';
    if (typeof event === 'string') {
      value = event;
    } else if (event && typeof event.detail === 'string') {
      value = event.detail;
    } else if (event && event.target && typeof event.target.value === 'string') {
      value = event.target.value;
    } else {
      value = String(event);
    }
    this.requestBodyEditorModel = {
      ...this.requestBodyEditorModel,
      value
    };
  }

  /**
   * Navigates back to the previous wizard step.
   */
  onBack() {
    this.backStep.emit();
  }

  /**
   * Finishes the wizard and emits the completion event.
   */
  onFinish() {
    this.finish.emit();
  }

  /**
   * Imports endpoints from the OpenAPI spec.
   */
  async importEndpoints(): Promise<void> {
    this.importing = true;
    try {
      let spec: any | null = null;
      let endpoints: any[] = [];

      // 1) Prefer locally stored spec content (uploaded file case)
      if (this.currentOpenApiSpec && typeof this.currentOpenApiSpec === 'string' && this.currentOpenApiSpec.trim()) {
        try {
          try {
            spec = JSON.parse(this.currentOpenApiSpec);
          } catch {
            spec = parseYAML(this.currentOpenApiSpec as string);
          }
          endpoints = this.openapi.discoverEndpointsFromSpec(spec);
        } catch (error) {
          console.error('[ManageEndpoints] Error parsing local spec:', error);
          spec = null;
          endpoints = [];
        }
      }

      // 2) Fallback to URL discovery when no local spec is available
      if ((!spec || endpoints.length === 0) && this.apiUrl) {
        endpoints = await this.openapi.discoverEndpointsFromSpecUrl(this.apiUrl);
        spec = await this.loadSpecCandidates(this.apiUrl);
      }
      // 3) Filter out duplicates by METHOD + PATH (matches backend unique key)
      const existing = await firstValueFrom(this.http.get<SourceSystemEndpointDTO[]>(`/api/config/source-system/${this.sourceSystemId}/endpoint`));
      const existingKeys = new Set(existing.map((e: any) => `${e.httpRequestType} ${e.endpointPath}`));
      const seenNew = new Set<string>();
      const toCreate = endpoints.filter((e: any) => {
        const key = `${e.httpRequestType} ${e.endpointPath}`;
        if (existingKeys.has(key) || seenNew.has(key)) return false;
        seenNew.add(key);
        return true;
      }).map((e: any) => ({
        ...e,
        sourceSystemId: this.sourceSystemId!
      }));

      // 4) Persist endpoints and discovered params if available
      const paramsByKey = spec ? this.openapi.discoverParamsFromSpec(spec) : {};
      if (toCreate.length) {
        const created = await firstValueFrom(this.http.post<SourceSystemEndpointDTO[]>(`/api/config/source-system/${this.sourceSystemId}/endpoint`, toCreate));
        const createdList = (created as any[]) || [];
        
        for (const ep of createdList) {
          const key = `${ep.httpRequestType} ${ep.endpointPath}`;
          const params = paramsByKey[key] || [];
          if (ep.id && params.length) {
            
            // Filter out duplicates within the same import batch
            const seenParams = new Set<string>();
            const uniqueParams = params.filter(p => {
              const paramKey = `${p.name}:${p.in === 'path' ? 'PATH' : 'QUERY'}`;
              if (seenParams.has(paramKey)) {
                return false;
              }
              seenParams.add(paramKey);
              return true;
            });
            
            if (uniqueParams.length > 0) {
              await this.openapi.persistParamsForEndpoint(ep.id, uniqueParams);
            }
          }
        }
        this.loadEndpoints();
      } else {
      }
    } catch (error) {
      console.error('[ManageEndpoints] Import failed:', error);
    } finally {
      this.importing = false;
    }
  }

  private async loadSpecCandidates(baseUrl: string): Promise<any> {
    const urls = [
      baseUrl,
      `${baseUrl}/swagger.json`,
      `${baseUrl}/openapi.json`,
      `${baseUrl}/v2/swagger.json`,
      `${baseUrl}/api/v3/openapi.json`,
      `${baseUrl}/swagger/v1/swagger.json`,
      `${baseUrl}/api-docs`,
      `${baseUrl}/openapi.yaml`,
      `${baseUrl}/openapi.yml`
    ];
    for (const url of urls) {
      try {
        const isJson = url.endsWith('.json') || (!url.endsWith('.yaml') && !url.endsWith('.yml'));
        const raw = await firstValueFrom(this.http.get(url, { responseType: isJson ? 'json' : 'text' as 'json' }));
        const spec: any = isJson ? raw : parseYAML(raw as string);
        if (spec && spec.paths) return spec;
      } catch { /* try next */ }
    }
    return null;
  }

  // Removed legacy OpenAPI import and processing methods in favor of shared OpenApiImportService

  /**
   * Ensures a parameter name is wrapped in curly braces {paramName}.
   */
  private ensureBraces(paramName: string): string {
    const cleanName = paramName.replace(/[{}]/g, '');
    return `{${cleanName}}`;
  }

  /**
   * Extracts path parameter names from an endpoint path.
   */
  private extractPathParameters(endpointPath: string): string[] {
    const pathParamRegex = /\{([^}]+)\}/g;
    const matches: string[] = [];
    let match: RegExpExecArray | null;
    while ((match = pathParamRegex.exec(endpointPath)) !== null) {
      matches.push(this.ensureBraces(match[1]));
    }
    const altPatterns = [
      /:(\w+)/g,
      /<(\w+)>/g,
      /\[(\w+)\]/g
    ];
    altPatterns.forEach(pattern => {
      let m: RegExpExecArray | null;
      while ((m = pattern.exec(endpointPath)) !== null) {
        const withBraces = this.ensureBraces(m[1]);
        if (!matches.includes(withBraces)) {
          matches.push(withBraces);
        }
      }
    });
    return matches;
  }

  /**
   * Saves discovered endpoints to the backend.
   */
  private saveDiscoveredEndpoints(discoveredEndpoints: Array<{endpoint: any, pathParams: string[]}>) {
    if (discoveredEndpoints.length === 0) {
      this.importing = false;
      return;
    }
    const endpointDTOs = discoveredEndpoints.map(item => ({
      ...item.endpoint,
      sourceSystemId: this.sourceSystemId!
    }));
    this.http.post<SourceSystemEndpointDTO[]>(`/api/config/source-system/${this.sourceSystemId}/endpoint`, endpointDTOs)
      .subscribe({
        next: () => {
          this.loadEndpoints();
          setTimeout(() => {}, 1000);
        },
        error: (error) => {
          console.error('[ManageEndpoints] Error saving discovered endpoints:', error);
          this.importing = false;
        }
      });
  }

  /**
   * Validator to ensure path parameter format: starts with { and ends with } with at least one character inside.
   */
  private pathParamFormatValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const val = control.value as string;
      if (typeof val === 'string' && val.match(/^\{[A-Za-z0-9_]+\}$/)) {
        return null;
      }
      return { invalidPathParamFormat: true };
    };
  }

  /**
   * Formats a path with parameters.
   */
  private formatPathWithParameters(path: string, parameters: any[]): string {
    let formattedPath = path;
    const pathParams = parameters?.filter(param => param.in === 'path') || [];
    pathParams.forEach(param => {
      const paramName = param.name;
      const patterns = [
        { regex: new RegExp(`:${paramName}\\b`, 'g'), replacement: `{${paramName}}` },
        { regex: new RegExp(`<${paramName}>`, 'g'), replacement: `{${paramName}}` },
        { regex: new RegExp(`\\[${paramName}\\]`, 'g'), replacement: `{${paramName}}` },
        {
          regex: new RegExp(`(?<!\\{|:|<|\\[)\\b${paramName}\\b(?!\\}|>|\\])(?=/|$)`, 'g'),
          replacement: `{${paramName}}`
        }
      ];
      patterns.forEach(pattern => {
        formattedPath = formattedPath.replace(pattern.regex, pattern.replacement);
      });
    });
    return formattedPath;
  }

  /**
   * Recursively resolves $ref in OpenAPI schemas.
   */
  private resolveRefs(schema: any, schemas: any, seen = new Set()): any {
    if (!schema) return schema;
    if (schema.$ref) {
      if (seen.has(schema.$ref)) return {};
      seen.add(schema.$ref);
      const refPath = schema.$ref.replace(/^#\//, '').split('/');
      let resolved = schemas;
      for (const part of refPath.slice(2)) {
        resolved = resolved?.[part];
      }
      if (!resolved) return {};
      return this.resolveRefs(resolved, schemas, seen);
    }
    if (schema.properties) {
      const newProps: any = {};
      for (const [key, value] of Object.entries(schema.properties)) {
        newProps[key] = this.resolveRefs(value, schemas, seen);
      }
      return { ...schema, properties: newProps };
    }
    if (schema.items) {
      return { ...schema, items: this.resolveRefs(schema.items, schemas, seen) };
    }
    for (const keyword of ['allOf', 'anyOf', 'oneOf']) {
      if (schema[keyword]) {
        return {
          ...schema,
          [keyword]: schema[keyword].map((s: any) => this.resolveRefs(s, schemas, seen))
        };
      }
    }
    if (typeof schema.additionalProperties === 'object' && schema.additionalProperties !== null) {
      return {
        ...schema,
        additionalProperties: this.resolveRefs(schema.additionalProperties, schemas, seen)
      };
    }
    return schema;
  }

  public currentOpenApiSpec: string | any = '';
  addEndpointVisible: boolean = false;

  /**
   * Resolves a $ref schema from the current OpenAPI spec (recursively).
   */
  private resolveSchemaFromOpenApi(schemaRef: any): any {
    if (!this.currentOpenApiSpec || !schemaRef.includes('$ref')) {
      try {
        return typeof schemaRef === 'string' ? JSON.parse(schemaRef) : schemaRef;
      } catch {
        return schemaRef;
      }
    }
    try {
      const openApiJson = typeof this.currentOpenApiSpec === 'string'
        ? JSON.parse(this.currentOpenApiSpec)
        : this.currentOpenApiSpec;
      const ref = typeof schemaRef === 'string'
        ? JSON.parse(schemaRef).$ref
        : schemaRef.$ref;
      if (ref && ref.startsWith('#/components/schemas/')) {
        const schemaName = ref.replace('#/components/schemas/', '');
        const schema = openApiJson.components?.schemas?.[schemaName];
        if (schema) {
          if (schema.$ref) {
            return this.resolveSchemaFromOpenApi(JSON.stringify(schema));
          }
          return schema;
        }
      }
    } catch (e) {}
    return schemaRef;

  }

  /**
   * Get tooltip text for Add Endpoint button
   */
  getAddEndpointTooltip(): string {
    if (this.endpointForm.valid) {
      return '';
    }

    const errors: string[] = [];
    
    // Check endpoint path
    const pathControl = this.endpointForm.get('endpointPath');
    if (pathControl?.invalid) {
      if (pathControl.errors?.['required']) {
        errors.push('Path is required');
      } else if (pathControl.errors?.['pathFormat']) {
        errors.push('Path must start with /');
      }
    }

    // Check request body schema
    const requestBodyControl = this.endpointForm.get('requestBodySchema');
    if (requestBodyControl?.invalid && requestBodyControl.errors?.['jsonFormat']) {
      errors.push('Invalid JSON in Request Body Schema');
    }

    // Check response body schema
    const responseBodyControl = this.endpointForm.get('responseBodySchema');
    if (responseBodyControl?.invalid && responseBodyControl.errors?.['jsonFormat']) {
      errors.push('Invalid JSON in Response Body Schema');
    }

    return errors.length > 0 ? errors.join(', ') : '';
  }

}
