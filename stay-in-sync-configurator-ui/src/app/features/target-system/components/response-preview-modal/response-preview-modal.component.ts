import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { MonacoEditorModule, NgxEditorModel } from 'ngx-monaco-editor-v2';
import { HttpClient } from '@angular/common/http';
import { TargetSystemEndpointResourceService } from '../../service/targetSystemEndpointResource.service';
import { TypeScriptGenerationRequest } from '../../../source-system/models/typescriptGenerationRequest';
import { TypeScriptGenerationResponse } from '../../../source-system/models/typescriptGenerationResponse';

/**
 * Component for previewing response body schemas and generated TypeScript interfaces
 * for Target System endpoints. Supports JSON schema validation, async TypeScript
 * generation via backend, and automatic tab switching.
 */
@Component({
  selector: 'app-target-response-preview-modal',
  standalone: true,
  imports: [
    CommonModule,
    DialogModule,
    ButtonModule,
    ProgressSpinnerModule,
    MonacoEditorModule
  ],
  templateUrl: './response-preview-modal.component.html',
  styleUrls: ['./response-preview-modal.component.css']
})
export class TargetResponsePreviewModalComponent implements OnInit, OnChanges {
  @Input() visible: boolean = false;
  @Input() endpointId: number | null | undefined = null;
  @Input() endpointPath: string = '';
  @Input() httpMethod: string = '';
  @Input() responseBodySchema: string | null | undefined = null;
  @Input() responseDts: string | null | undefined = null;
  @Output() visibleChange = new EventEmitter<boolean>();

  jsonEditorOptions = {
    theme: 'vs-dark',
    language: 'json',
    automaticLayout: true,
    readOnly: true,
    minimap: { enabled: false },
    wordWrap: 'on'
  } as const;
  typescriptEditorOptions = {
    theme: 'vs-dark',
    language: 'typescript',
    automaticLayout: true,
    readOnly: true,
    minimap: { enabled: false },
    wordWrap: 'on'
  } as const;
  jsonEditorModel: NgxEditorModel = { value: '', language: 'json' };
  typescriptEditorModel: NgxEditorModel = { value: '', language: 'typescript' };
  activeTabIndex: number = 0;
  loading = false;
  error: string | null = null;
  isGeneratingTypeScript: boolean = false;
  typescriptError: string | null = null;
  generatedTypeScript: string = '';
  private readonly TYPESCRIPT_GENERATION_TIMEOUT = 30000;
  private typescriptGenerationTimeout: any = null;

  constructor(
    private endpointSvc: TargetSystemEndpointResourceService,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  /**
   * Initializes the component by setting the initial editor models.
   * Automatically switches to the TypeScript tab if schema or DTS exists.
   */
  ngOnInit(): void {
    this.updateEditorModel();
    if (this.responseBodySchema || this.responseDts) {
      setTimeout(() => this.setActiveTab(1), 0);
    }
  }

  /**
   * Reacts to input changes (like endpoint ID or schema updates),
   * resets TypeScript generation state, and refreshes editor models.
   * @param changes Angular SimpleChanges containing updated input values.
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
      if (this.activeTabIndex === 1) {
        this.onTabChange({ index: 1 });
      }
    }, 100);
  }

  /**
   * Forces tab change and triggers TypeScript regeneration if necessary.
   * @param tabIndex Index of the target tab.
   */
  private forceTabChange(tabIndex: number): void {
    this.activeTabIndex = tabIndex;
    this.cdr.detectChanges();
    this.onTabChange({ index: tabIndex });
  }

  /**
   * Sets the active tab manually and triggers appropriate refresh logic.
   * @param tabIndex Index of the selected tab.
   */
  setActiveTab(tabIndex: number): void {
    this.activeTabIndex = tabIndex;
    this.onTabChange({ index: tabIndex });
  }

  /**
   * Handles logic when the user switches between JSON and TypeScript tabs.
   * Automatically loads TypeScript if not yet generated.
   * @param event Tab change event object.
   */
  onTabChange(event: any): void {
    this.activeTabIndex = event.index;
    if (event.index === 1) {
      if (this.responseDts) {
        this.typescriptEditorModel = { value: this.responseDts, language: 'typescript' };
        this.isGeneratingTypeScript = false;
        this.typescriptError = null;
        return;
      }
      if (!this.responseDts) {
        if (!this.generatedTypeScript && !this.isGeneratingTypeScript) {
          this.loadTypeScript();
        } else if (this.generatedTypeScript && !this.typescriptEditorModel.value) {
          this.typescriptEditorModel = { value: this.generatedTypeScript, language: 'typescript' };
        }
      }
    }
  }

  /**
   * Calls backend service to generate TypeScript from the given JSON schema.
   * Handles validation, timeouts, and fallback error messages.
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
    const request: TypeScriptGenerationRequest = { jsonSchema: this.responseBodySchema };
    this.endpointSvc.generateTypeScript(this.endpointId, request).subscribe({
      next: (response: TypeScriptGenerationResponse) => {
        this.clearTypeScriptGenerationTimeout();
        this.isGeneratingTypeScript = false;
        this.generatedTypeScript = response.generatedTypeScript || '';
        this.typescriptEditorModel = { value: response.generatedTypeScript || '', language: 'typescript' };
      },
      error: (error) => {
        this.clearTypeScriptGenerationTimeout();
        this.isGeneratingTypeScript = false;
        this.generatedTypeScript = this.getTypeScriptErrorFallback(this.responseBodySchema || '');
        this.typescriptEditorModel = { value: this.generatedTypeScript, language: 'typescript' };
        this.typescriptError = this.formatErrorMessage(error.message || 'Unknown error', 'TypeScript Generation');
      }
    });
  }

  /**
   * Handles timeout if TypeScript generation exceeds allowed duration.
   * Generates fallback TypeScript structure and updates UI state.
   */
  private handleTypeScriptGenerationTimeout(): void {
    this.isGeneratingTypeScript = false;
    this.typescriptError = 'TypeScript generation timed out after 30 seconds';
    this.generatedTypeScript = this.getTypeScriptErrorFallback(this.responseBodySchema || '');
    this.typescriptEditorModel = { value: this.generatedTypeScript, language: 'typescript' };
  }

  /** Clears the running TypeScript generation timeout if active. */
  private clearTypeScriptGenerationTimeout(): void {
    if (this.typescriptGenerationTimeout) {
      clearTimeout(this.typescriptGenerationTimeout);
      this.typescriptGenerationTimeout = null;
    }
  }

  /**
   * Formats backend or parsing errors into a readable message string.
   * @param error The original error message.
   * @param context The logical context where the error occurred.
   * @returns Formatted readable error string.
   */
  private formatErrorMessage(error: string, context: string): string {
    return `${context} failed: ${error}`;
  }

  /**
   * Generates a fallback TypeScript interface when generation fails.
   * @param jsonSchema The JSON schema snippet to embed in the comment.
   * @returns Fallback TypeScript interface as string.
   */
  private getTypeScriptErrorFallback(jsonSchema: string): string {
    return `// TypeScript generation failed\n// Fallback interface based on JSON schema\n// Original schema: ${jsonSchema.substring(0, 100)}...\n\nexport interface ResponseBody {\n  [key: string]: any;\n}`;
  }

  /**
   * Validates JSON schema syntax before attempting TypeScript generation.
   * @param jsonSchema JSON string to validate.
   * @returns Object indicating validity and optional error message.
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
   * Updates both JSON and TypeScript Monaco editor models
   * based on the provided schema or DTS content.
   */
  private updateEditorModel(): void {
    if (this.responseBodySchema) {
      this.jsonEditorModel = { value: this.responseBodySchema, language: 'json' };
    } else {
      this.jsonEditorModel = { value: '', language: 'json' };
    }
    if (this.responseDts) {
      this.typescriptEditorModel = { value: this.responseDts, language: 'typescript' };
      this.generatedTypeScript = this.responseDts;
      if (this.activeTabIndex !== 1) {
        this.activeTabIndex = 1;
      }
    } else {
      this.typescriptEditorModel = { value: '', language: 'typescript' };
      if (this.activeTabIndex !== 0) {
        this.activeTabIndex = 0;
      }
    }
    setTimeout(() => { this.cdr.detectChanges(); }, 0);
  }

  /**
   * Closes the preview modal and clears pending timeouts.
   * Emits visibility change event to parent component.
   */
  onClose(): void {
    this.clearTypeScriptGenerationTimeout();
    this.visible = false;
    this.visibleChange.emit(false);
  }

  /**
   * Determines if there is a valid response schema or DTS available.
   * @returns True if any response body content exists.
   */
  get hasResponseBody(): boolean {
    const hasSchema = !!this.responseBodySchema && this.responseBodySchema.trim().length > 0;
    const hasDts = !!this.responseDts && this.responseDts.trim().length > 0;
    return hasSchema || hasDts;
  }

  /**
   * Dynamically constructs the modal title based on HTTP method and endpoint path.
   * @returns Formatted modal title string.
   */
  get modalTitle(): string {
    return `Response Body Schema - ${this.httpMethod} ${this.endpointPath}`;
  }
}
