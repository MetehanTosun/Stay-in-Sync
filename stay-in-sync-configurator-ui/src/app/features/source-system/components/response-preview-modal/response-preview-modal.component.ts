import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import { HttpClient } from '@angular/common/http';
import { SourceSystemEndpointResourceService } from '../../service/sourceSystemEndpointResource.service';
import { TypeScriptGenerationRequest } from '../../models/typescriptGenerationRequest';
import { TypeScriptGenerationResponse } from '../../models/typescriptGenerationResponse';

@Component({
  selector: 'app-response-preview-modal',
  standalone: true,
  imports: [
    CommonModule,
    DialogModule,
    ButtonModule,
    ProgressSpinnerModule
  ],
  templateUrl: './response-preview-modal.component.html',
  styleUrls: ['./response-preview-modal.component.css']
})
export class ResponsePreviewModalComponent implements OnInit, OnChanges {
  /**
   * Controls the visibility of the modal.
   */
  @Input() visible: boolean = false;

  /**
   * ID of the endpoint associated with the modal.
   */
  @Input() endpointId: number | null | undefined = null;

  /**
   * Path of the endpoint associated with the modal.
   */
  @Input() endpointPath: string = '';

  /**
   * HTTP method of the endpoint.
   */
  @Input() httpMethod: string = '';

  /**
   * JSON schema of the response body.
   */
  @Input() responseBodySchema: string | null | undefined = null;

  /**
   * TypeScript definition of the response body.
   */
  @Input() responseDts: string | null | undefined = null;

  /**
   * Emits an event when the visibility of the modal changes.
   */
  @Output() visibleChange = new EventEmitter<boolean>();

  /**
   * Model for the JSON editor.
   */
  jsonEditorModel = { value: '' };

  /**
   * Model for the TypeScript editor.
   */
  typescriptEditorModel = { value: '' };

  /**
   * Index of the currently active tab.
   */
  activeTabIndex: number = 0;

  /**
   * Indicates whether the modal is loading data.
   */
  loading = false;

  /**
   * Error message for the modal.
   */
  error: string | null = null;

  /**
   * Indicates whether TypeScript generation is in progress.
   */
  isGeneratingTypeScript: boolean = false;

  /**
   * Error message for TypeScript generation.
   */
  typescriptError: string | null = null;

  /**
   * Generated TypeScript definition.
   */
  generatedTypeScript: string = '';

  /**
   * Timeout duration for TypeScript generation.
   */
  private readonly TYPESCRIPT_GENERATION_TIMEOUT = 30000;

  /**
   * Timeout reference for TypeScript generation.
   */
  private typescriptGenerationTimeout: any = null;

  constructor(
    private endpointSvc: SourceSystemEndpointResourceService,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  /**
   * Initializes the component and updates the editor models.
   */
  ngOnInit(): void {
    this.updateEditorModel();
  }

  /**
   * Updates the component when input properties change.
   * @param changes The changes to the input properties.
   */
  ngOnChanges(changes: SimpleChanges): void {
    this.updateEditorModel();
    this.clearTypeScriptGenerationTimeout();
    this.isGeneratingTypeScript = false;
    this.typescriptError = null;
    this.generatedTypeScript = '';
    this.cdr.detectChanges();

    setTimeout(() => {
      this.cdr.detectChanges();
      if (this.responseDts && this.activeTabIndex === 1) {
        this.forceTabChange(1);
      }
    }, 100);
  }

  /**
   * Changes the active tab programmatically.
   * @param tabIndex The index of the tab to activate.
   */
  private forceTabChange(tabIndex: number): void {
    this.activeTabIndex = tabIndex;
    this.cdr.detectChanges();
    this.onTabChange({ index: tabIndex });
  }

  /**
   * Sets the active tab.
   * @param tabIndex The index of the tab to activate.
   */
  setActiveTab(tabIndex: number): void {
    this.activeTabIndex = tabIndex;
    this.onTabChange({ index: tabIndex });
  }

  /**
   * Handles tab change events and triggers TypeScript generation if needed.
   * @param event The tab change event.
   */
  onTabChange(event: any): void {
    this.activeTabIndex = event.index;
    if (event.index === 1) {
      if (this.responseDts) {
        this.typescriptEditorModel = { value: this.responseDts };
        this.isGeneratingTypeScript = false;
        this.typescriptError = null;
        return;
      }
      if (!this.responseDts) {
        if (!this.generatedTypeScript && !this.isGeneratingTypeScript) {
          this.loadTypeScript();
        } else if (this.generatedTypeScript && !this.typescriptEditorModel.value) {
          this.typescriptEditorModel = { value: this.generatedTypeScript };
        }
      }
    }
  }

  /**
   * Loads the TypeScript interface from the backend.
   */
  loadTypeScript(): void {
    if (!this.responseBodySchema || !this.endpointId) {
      this.typescriptError = 'Cannot generate TypeScript: missing schema or endpoint ID';
      return;
    }

    this.isGeneratingTypeScript = true;
    this.typescriptError = null;
    this.generatedTypeScript = '';

    this.typescriptGenerationTimeout = setTimeout(() => {
      this.handleTypeScriptGenerationTimeout();
    }, this.TYPESCRIPT_GENERATION_TIMEOUT);

    const validation = this.validateJsonSchema(this.responseBodySchema);
    if (!validation.isValid) {
      this.isGeneratingTypeScript = false;
      this.clearTypeScriptGenerationTimeout();
      this.typescriptError = `Invalid JSON schema: ${validation.error}`;
      return;
    }

    const request: TypeScriptGenerationRequest = {
      jsonSchema: this.responseBodySchema
    };

    this.endpointSvc.generateTypeScript(this.endpointId, request).subscribe({
      next: (response: TypeScriptGenerationResponse) => {
        this.clearTypeScriptGenerationTimeout();
        this.isGeneratingTypeScript = false;
        this.generatedTypeScript = response.generatedTypeScript || '';
        this.typescriptEditorModel = { value: response.generatedTypeScript || '' };
      },
      error: (error) => {
        this.clearTypeScriptGenerationTimeout();
        this.isGeneratingTypeScript = false;
        this.generatedTypeScript = this.getTypeScriptErrorFallback(this.responseBodySchema || '');
        this.typescriptEditorModel = { value: this.generatedTypeScript };
        this.typescriptError = this.formatErrorMessage(error.message || 'Unknown error', 'TypeScript Generation');
      }
    });
  }

  /**
   * Handles TypeScript generation timeout.
   */
  private handleTypeScriptGenerationTimeout(): void {
    this.isGeneratingTypeScript = false;
    this.typescriptError = 'TypeScript generation timed out after 30 seconds';
    this.generatedTypeScript = this.getTypeScriptErrorFallback(this.responseBodySchema || '');
    this.typescriptEditorModel = { value: this.generatedTypeScript };
  }

  /**
   * Clears the TypeScript generation timeout.
   */
  private clearTypeScriptGenerationTimeout(): void {
    if (this.typescriptGenerationTimeout) {
      clearTimeout(this.typescriptGenerationTimeout);
      this.typescriptGenerationTimeout = null;
    }
  }

  /**
   * Formats an error message for display.
   * @param error The error message.
   * @param context The context of the error.
   * @returns The formatted error message.
   */
  private formatErrorMessage(error: string, context: string): string {
    return `${context} failed: ${error}`;
  }

  /**
   * Provides a fallback TypeScript interface when generation fails.
   * @param jsonSchema The JSON schema.
   * @returns The fallback TypeScript interface.
   */
  private getTypeScriptErrorFallback(jsonSchema: string): string {
    return `// TypeScript generation failed
// Fallback interface based on JSON schema
// Original schema: ${jsonSchema.substring(0, 100)}...

export interface ResponseBody {
  [key: string]: any;
}`;
  }

  /**
   * Validates the JSON schema.
   * @param jsonSchema The JSON schema.
   * @returns The validation result.
   */
  private validateJsonSchema(jsonSchema: string): { isValid: boolean; error?: string } {
    try {
      JSON.parse(jsonSchema);
      return { isValid: true };
    } catch (error) {
      return { isValid: false, error: error instanceof Error ? error.message : 'Invalid JSON' };
    }
  }

  /**
   * Updates the editor models with the current response body schema.
   */
  private updateEditorModel(): void {
    if (this.responseBodySchema) {
      this.jsonEditorModel = { value: this.responseBodySchema };
    } else {
      this.jsonEditorModel = { value: '// No JSON schema available' };
    }

    if (this.responseDts) {
      this.typescriptEditorModel = { value: this.responseDts };
      this.generatedTypeScript = this.responseDts;
      if (this.activeTabIndex !== 1) {
        this.activeTabIndex = 1;
      }
    } else {
      this.typescriptEditorModel = { value: '// Click on TypeScript tab to generate interface' };
      if (this.activeTabIndex !== 0) {
        this.activeTabIndex = 0;
      }
    }

    setTimeout(() => {
      this.cdr.detectChanges();
    }, 0);
  }

  /**
   * Closes the modal and emits the visibility change event.
   */
  onClose(): void {
    this.clearTypeScriptGenerationTimeout();
    this.visible = false;
    this.visibleChange.emit(false);
  }

  /**
   * Checks whether the response body is available.
   * @returns True if the response body is available, false otherwise.
   */
  get hasResponseBody(): boolean {
    const hasSchema = !!this.responseBodySchema && this.responseBodySchema.trim().length > 0;
    const hasDts = !!this.responseDts && this.responseDts.trim().length > 0;
    return hasSchema || hasDts;
  }

  /**
   * Gets the title of the modal.
   * @returns The modal title.
   */
  get modalTitle(): string {
    return `Response Body Schema - ${this.httpMethod} ${this.endpointPath}`;
  }
}