import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { HttpClient } from '@angular/common/http';
import { TargetSystemEndpointResourceService } from '../../service/targetSystemEndpointResource.service';

@Component({
  selector: 'app-target-response-preview-modal',
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
export class TargetResponsePreviewModalComponent implements OnInit, OnChanges {
  @Input() visible: boolean = false;
  @Input() endpointId: number | null | undefined = null;
  @Input() endpointPath: string = '';
  @Input() httpMethod: string = '';
  @Input() responseBodySchema: string | null | undefined = null;
  @Input() responseDts: string | null | undefined = null;
  @Output() visibleChange = new EventEmitter<boolean>();

  jsonEditorModel = { value: '' };
  typescriptEditorModel = { value: '' };
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

  ngOnInit(): void {
    this.updateEditorModel();
  }

  ngOnChanges(changes: SimpleChanges): void {
    this.updateEditorModel();
    this.clearTypeScriptGenerationTimeout();
    this.isGeneratingTypeScript = false;
    this.typescriptError = null;
    this.generatedTypeScript = '';
    this.cdr.detectChanges();
    setTimeout(() => {
      this.cdr.detectChanges();
      // If user already on TS tab, ensure generation kicks in when opening
      if (this.activeTabIndex === 1) {
        this.onTabChange({ index: 1 });
      }
    }, 100);
  }

  private forceTabChange(tabIndex: number): void {
    this.activeTabIndex = tabIndex;
    this.cdr.detectChanges();
    this.onTabChange({ index: tabIndex });
  }

  setActiveTab(tabIndex: number): void {
    this.activeTabIndex = tabIndex;
    this.onTabChange({ index: tabIndex });
  }

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
    const request = { jsonSchema: this.responseBodySchema } as any;
    this.endpointSvc.generateTypeScript(this.endpointId, request).subscribe({
      next: (response: any) => {
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

  private handleTypeScriptGenerationTimeout(): void {
    this.isGeneratingTypeScript = false;
    this.typescriptError = 'TypeScript generation timed out after 30 seconds';
    this.generatedTypeScript = this.getTypeScriptErrorFallback(this.responseBodySchema || '');
    this.typescriptEditorModel = { value: this.generatedTypeScript };
  }

  private clearTypeScriptGenerationTimeout(): void {
    if (this.typescriptGenerationTimeout) {
      clearTimeout(this.typescriptGenerationTimeout);
      this.typescriptGenerationTimeout = null;
    }
  }

  private formatErrorMessage(error: string, context: string): string {
    return `${context} failed: ${error}`;
  }

  private getTypeScriptErrorFallback(jsonSchema: string): string {
    return `// TypeScript generation failed\n// Fallback interface based on JSON schema\n// Original schema: ${jsonSchema.substring(0, 100)}...\n\nexport interface ResponseBody {\n  [key: string]: any;\n}`;
  }

  private validateJsonSchema(jsonSchema: string): { isValid: boolean; error?: string } {
    try {
      JSON.parse(jsonSchema);
      return { isValid: true };
    } catch (error) {
      return { isValid: false, error: error instanceof Error ? error.message : 'Invalid JSON' };
    }
  }

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
    setTimeout(() => { this.cdr.detectChanges(); }, 0);
  }

  onClose(): void {
    this.clearTypeScriptGenerationTimeout();
    this.visible = false;
    this.visibleChange.emit(false);
  }

  get hasResponseBody(): boolean {
    const hasSchema = !!this.responseBodySchema && this.responseBodySchema.trim().length > 0;
    const hasDts = !!this.responseDts && this.responseDts.trim().length > 0;
    return hasSchema || hasDts;
  }

  get modalTitle(): string {
    return `Response Body Schema - ${this.httpMethod} ${this.endpointPath}`;
  }
}


