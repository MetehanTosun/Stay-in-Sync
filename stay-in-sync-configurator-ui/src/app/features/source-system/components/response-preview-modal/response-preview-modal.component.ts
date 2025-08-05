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
  @Input() visible: boolean = false;
  @Input() endpointId: number | null | undefined = null;
  @Input() endpointPath: string = '';
  @Input() httpMethod: string = '';
  @Input() responseBodySchema: string | null | undefined = null;
  @Input() responseDts: string | null | undefined = null;
  @Output() visibleChange = new EventEmitter<boolean>();

  // Editor models for textareas
  jsonEditorModel = { value: '' };
  typescriptEditorModel = { value: '' };
  
  // Tab state
  activeTabIndex: number = 0;
  
  // Loading and error states
  loading = false;
  error: string | null = null;
  
  // TypeScript generation states
  isGeneratingTypeScript: boolean = false;
  typescriptError: string | null = null;
  generatedTypeScript: string = '';
  
  // Timeout settings
  private readonly TYPESCRIPT_GENERATION_TIMEOUT = 30000; // 30 seconds
  private typescriptGenerationTimeout: any = null;
  
  // Content for display - removed typescriptContent property

  constructor(
    private endpointSvc: SourceSystemEndpointResourceService,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    console.log('[ResponsePreviewModal] ngOnInit called');
    this.updateEditorModel();
  }

  /**
   * Log component state for debugging
   */
  private logComponentState(): void {
    console.log('[ResponsePreviewModal] === Component State ===');
    console.log('[ResponsePreviewModal] visible:', this.visible);
    console.log('[ResponsePreviewModal] endpointId:', this.endpointId);
    console.log('[ResponsePreviewModal] endpointPath:', this.endpointPath);
    console.log('[ResponsePreviewModal] httpMethod:', this.httpMethod);
    console.log('[ResponsePreviewModal] responseBodySchema:', this.responseBodySchema?.substring(0, 100) + '...');
    console.log('[ResponsePreviewModal] responseDts:', this.responseDts?.substring(0, 100) + '...');
    console.log('[ResponsePreviewModal] hasResponseBody:', this.hasResponseBody);
    console.log('[ResponsePreviewModal] activeTabIndex:', this.activeTabIndex);
    console.log('[ResponsePreviewModal] jsonEditorModel.value length:', this.jsonEditorModel.value.length);
    console.log('[ResponsePreviewModal] typescriptEditorModel.value length:', this.typescriptEditorModel.value.length);
    console.log('[ResponsePreviewModal] ========================');
  }

  /**
   * Force tab change programmatically
   */
  private forceTabChange(tabIndex: number): void {
    console.log('[ResponsePreviewModal] Force changing tab to index:', tabIndex);
    this.activeTabIndex = tabIndex;
    this.cdr.detectChanges();
    
    // Simulate the tab change event
    this.onTabChange({ index: tabIndex });
  }

  /**
   * Set active tab (public method for template)
   */
  setActiveTab(tabIndex: number): void {
    console.log('[ResponsePreviewModal] setActiveTab called with index:', tabIndex);
    this.activeTabIndex = tabIndex;
    this.onTabChange({ index: tabIndex });
  }

  ngOnChanges(changes: SimpleChanges): void {
    console.log('[ResponsePreviewModal] ngOnChanges called');
    this.updateEditorModel();
    
    // Reset TypeScript generation when schema changes
    this.clearTypeScriptGenerationTimeout();
    this.isGeneratingTypeScript = false;
    this.typescriptError = null;
    this.generatedTypeScript = '';
    
    // Log component state for debugging
    this.logComponentState();
    
    // Force change detection after model update
    this.cdr.detectChanges();

    // Add a longer delay to ensure PrimeNG TabView has fully rendered
    setTimeout(() => {
      console.log('[ResponsePreviewModal] Forcing change detection after timeout in ngOnChanges');
      this.cdr.detectChanges();
      
      // Force the tab to update if we have TypeScript data
      if (this.responseDts && this.activeTabIndex === 1) {
        console.log('[ResponsePreviewModal] Forcing TypeScript tab update');
        this.forceTabChange(1);
      }
    }, 100); // Increased delay to ensure TabView is fully rendered
  }

  /**
   * Handle tab change and generate TypeScript if needed
   */
  onTabChange(event: any): void {
    this.activeTabIndex = event.index;
    console.log('[ResponsePreviewModal] Tab changed to index:', event.index);
    
    // Wenn TypeScript tab aktiviert wird
    if (event.index === 1) {
      console.log('[ResponsePreviewModal] TypeScript tab activated');
      
      // Wenn responseDts vorhanden ist, nur Model setzen
      if (this.responseDts) {
        console.log('[ResponsePreviewModal] TypeScript tab with responseDts - setting model');
        this.typescriptEditorModel = { value: this.responseDts };
        this.isGeneratingTypeScript = false;
        this.typescriptError = null;
        return;
      }
      
      // Wenn kein responseDts, TypeScript generieren
      if (!this.responseDts) {
        console.log('[ResponsePreviewModal] TypeScript tab without responseDts - generating TypeScript');
        if (!this.generatedTypeScript && !this.isGeneratingTypeScript) {
          this.loadTypeScript();
        } else if (this.generatedTypeScript && !this.typescriptEditorModel.value) {
          this.typescriptEditorModel = { value: this.generatedTypeScript };
        }
      }
    }
  }

  /**
   * Load TypeScript interface from the backend
   */
  loadTypeScript(): void {
    if (!this.responseBodySchema || !this.endpointId) {
      console.warn('[ResponsePreviewModal] Cannot generate TypeScript: missing schema or endpoint ID');
      this.typescriptError = 'Cannot generate TypeScript: missing schema or endpoint ID';
      return;
    }

    console.log('[ResponsePreviewModal] Starting TypeScript generation for endpoint:', this.endpointId);
    
    this.isGeneratingTypeScript = true;
    this.typescriptError = null;
    this.generatedTypeScript = '';
    
    // Set timeout for TypeScript generation
    this.typescriptGenerationTimeout = setTimeout(() => {
      this.handleTypeScriptGenerationTimeout();
    }, this.TYPESCRIPT_GENERATION_TIMEOUT);

    // Validate JSON schema first
    const validation = this.validateJsonSchema(this.responseBodySchema);
    if (!validation.isValid) {
      console.warn('[ResponsePreviewModal] Invalid JSON schema:', validation.error);
      this.isGeneratingTypeScript = false;
      this.clearTypeScriptGenerationTimeout();
      this.typescriptError = `Invalid JSON schema: ${validation.error}`;
      return;
    }

    // Create request payload
    const request: TypeScriptGenerationRequest = {
      jsonSchema: this.responseBodySchema
    };

    console.log('[ResponsePreviewModal] Sending TypeScript generation request:', request);

    // Call the backend service
    this.endpointSvc.generateTypeScript(this.endpointId, request).subscribe({
      next: (response: TypeScriptGenerationResponse) => {
        console.log('[ResponsePreviewModal] TypeScript generation successful:', response);
        this.clearTypeScriptGenerationTimeout();
        this.isGeneratingTypeScript = false;
        this.generatedTypeScript = response.generatedTypeScript || '';
        this.typescriptEditorModel = { value: response.generatedTypeScript || '' };
      },
      error: (error) => {
        console.error('[ResponsePreviewModal] TypeScript generation failed:', error);
        this.clearTypeScriptGenerationTimeout();
        this.isGeneratingTypeScript = false;
        
        // Provide fallback TypeScript interface
        this.generatedTypeScript = this.getTypeScriptErrorFallback(this.responseBodySchema || '');
        this.typescriptEditorModel = { value: this.generatedTypeScript };
        
        // Set error message
        this.typescriptError = this.formatErrorMessage(error.message || 'Unknown error', 'TypeScript Generation');
      }
    });
  }

  /**
   * Handle TypeScript generation timeout
   */
  private handleTypeScriptGenerationTimeout(): void {
    console.warn('[ResponsePreviewModal] TypeScript generation timed out');
    this.isGeneratingTypeScript = false;
    this.typescriptError = 'TypeScript generation timed out after 30 seconds';
    
    // Provide fallback TypeScript interface
    this.generatedTypeScript = this.getTypeScriptErrorFallback(this.responseBodySchema || '');
    this.typescriptEditorModel = { value: this.generatedTypeScript };
  }

  /**
   * Clear TypeScript generation timeout
   */
  private clearTypeScriptGenerationTimeout(): void {
    if (this.typescriptGenerationTimeout) {
      clearTimeout(this.typescriptGenerationTimeout);
      this.typescriptGenerationTimeout = null;
    }
  }

  /**
   * Format error message for display
   */
  private formatErrorMessage(error: string, context: string): string {
    return `${context} failed: ${error}`;
  }

  /**
   * Get fallback TypeScript interface when generation fails
   */
  private getTypeScriptErrorFallback(jsonSchema: string): string {
    return `// TypeScript generation failed
// Fallback interface based on JSON schema
// Original schema: ${jsonSchema.substring(0, 100)}...

export interface ResponseBody {
  // Fallback interface - please check the JSON schema above
  [key: string]: any;
}`;
  }

  /**
   * Validate JSON schema
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
   * Update editor model with current response body schema
   */
  private updateEditorModel(): void {
    console.log('[ResponsePreviewModal] updateEditorModel called');
    console.log('[ResponsePreviewModal] responseBodySchema:', this.responseBodySchema);
    console.log('[ResponsePreviewModal] responseDts:', this.responseDts);
    
    // Set JSON model
    if (this.responseBodySchema) {
      console.log('[ResponsePreviewModal] Updating JSON editor model with schema');
      this.jsonEditorModel = { value: this.responseBodySchema };
    } else {
      console.log('[ResponsePreviewModal] No response body schema available');
      this.jsonEditorModel = { value: '// No JSON schema available' };
    }
    
    // Set TypeScript model if responseDts is available
    if (this.responseDts) {
      console.log('[ResponsePreviewModal] Updating TypeScript editor model with responseDts');
      this.typescriptEditorModel = { value: this.responseDts };
      this.generatedTypeScript = this.responseDts;
      console.log('[ResponsePreviewModal] typescriptEditorModel.value set to:', this.typescriptEditorModel.value.substring(0, 100) + '...');
      
      // If we have TypeScript data, set the active tab to TypeScript (index 1)
      if (this.activeTabIndex !== 1) {
        console.log('[ResponsePreviewModal] Setting active tab to TypeScript (index 1)');
        this.activeTabIndex = 1;
      }
    } else {
      console.log('[ResponsePreviewModal] No responseDts available, TypeScript model will be set when tab is clicked');
      this.typescriptEditorModel = { value: '// Click on TypeScript tab to generate interface' };
      
      // If no TypeScript data, default to JSON tab (index 0)
      if (this.activeTabIndex !== 0) {
        console.log('[ResponsePreviewModal] Setting active tab to JSON (index 0)');
        this.activeTabIndex = 0;
      }
    }
    
    // Force change detection after model updates
    setTimeout(() => {
      this.cdr.detectChanges();
    }, 0);
  }

  /**
   * Close the modal
   */
  onClose(): void {
    console.log('[ResponsePreviewModal] Closing modal');
    this.clearTypeScriptGenerationTimeout();
    this.visible = false;
    this.visibleChange.emit(false);
  }

  /**
   * Check if response body is available
   */
  get hasResponseBody(): boolean {
    const hasSchema = !!this.responseBodySchema && this.responseBodySchema.trim().length > 0;
    const hasDts = !!this.responseDts && this.responseDts.trim().length > 0;
    const result = hasSchema || hasDts;
    
    console.log('[ResponsePreviewModal] hasResponseBody check:');
    console.log('[ResponsePreviewModal] - hasSchema:', hasSchema);
    console.log('[ResponsePreviewModal] - hasDts:', hasDts);
    console.log('[ResponsePreviewModal] - result:', result);
    console.log('[ResponsePreviewModal] - responseBodySchema length:', this.responseBodySchema?.length || 0);
    console.log('[ResponsePreviewModal] - responseDts length:', this.responseDts?.length || 0);
    
    return result;
  }





  /**
   * Get modal title
   */
  get modalTitle(): string {
    return `Response Body Schema - ${this.httpMethod} ${this.endpointPath}`;
  }
} 