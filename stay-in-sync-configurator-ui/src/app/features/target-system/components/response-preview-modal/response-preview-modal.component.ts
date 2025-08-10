import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { HttpClient } from '@angular/common/http';
import { TargetSystemEndpointResourceService } from '../../service/targetSystemEndpointResource.service';
import { TypeScriptGenerationRequest } from '../../../source-system/models/typescriptGenerationRequest';
import { TypeScriptGenerationResponse } from '../../../source-system/models/typescriptGenerationResponse';

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
    const request: TypeScriptGenerationRequest = { jsonSchema: this.responseBodySchema };
    this.endpointSvc.generateTypeScript(this.endpointId, request).subscribe({
      next: (response: TypeScriptGenerationResponse) => {
        this.clearTypeScriptGenerationTimeout();
        this.isGeneratingTypeScript = false;
        if (response.error) {
          this.typescriptError = this.formatErrorMessage(response.error, 'Backend generation failed');
          this.generatedTypeScript = this.generateTypeScriptFallback(this.responseBodySchema || '');
          this.typescriptEditorModel = { value: this.generatedTypeScript };
          return;
        }
        this.generatedTypeScript = response.generatedTypeScript || '';
        this.typescriptEditorModel = { value: response.generatedTypeScript || this.generateTypeScriptFallback(this.responseBodySchema || '') };
      },
      error: (error) => {
        this.clearTypeScriptGenerationTimeout();
        this.isGeneratingTypeScript = false;
        this.generatedTypeScript = this.generateTypeScriptFallback(this.responseBodySchema || '');
        this.typescriptEditorModel = { value: this.generatedTypeScript };
        this.typescriptError = this.formatErrorMessage(error.message || 'Unknown error', 'TypeScript Generation');
      }
    });
  }

  private handleTypeScriptGenerationTimeout(): void {
    this.isGeneratingTypeScript = false;
    this.typescriptError = 'TypeScript generation timed out after 30 seconds';
    this.generatedTypeScript = this.generateTypeScriptFallback(this.responseBodySchema || '');
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

  private generateTypeScriptFallback(jsonSchema: string): string {
    try {
      const schema = JSON.parse(jsonSchema);
      return this.convertJsonSchemaToTypeScript(schema);
    } catch {
      return `// TypeScript generation failed\n// Invalid JSON schema. Showing generic fallback.\nexport interface ResponseBody {\n  [key: string]: any;\n}`;
    }
  }

  private validateJsonSchema(jsonSchema: string): { isValid: boolean; error?: string } {
    try {
      JSON.parse(jsonSchema);
      return { isValid: true };
    } catch (error) {
      return { isValid: false, error: error instanceof Error ? error.message : 'Invalid JSON' };
    }
  }

  // Minimal JSON-Schema -> TypeScript converter (mirrors Source fallback)
  private convertJsonSchemaToTypeScript(schema: any): string {
    const toTs = (s: any): string => {
      if (!s || typeof s !== 'object') return 'any';
      if (s.$ref) return 'any';
      switch (s.type) {
        case 'string':
          if (Array.isArray(s.enum)) return s.enum.map((v: any) => `'${String(v)}'`).join(' | ');
          return 'string';
        case 'number':
        case 'integer':
          if (Array.isArray(s.enum)) return s.enum.join(' | ');
          return 'number';
        case 'boolean':
          return 'boolean';
        case 'array':
          return `${toTs(s.items || {})}[]`;
        case 'object':
          if (s.properties) {
            const req = Array.isArray(s.required) ? new Set(s.required) : new Set<string>();
            const lines = Object.entries(s.properties).map(([key, val]: [string, any]) => {
              const optional = req.has(key) ? '' : '?';
              return `  ${key}${optional}: ${toTs(val)};`;
            });
            return `{
${lines.join('\n')}
}`;
          }
          return 'Record<string, any>';
        default:
          return 'any';
      }
    };
    const body = toTs(schema);
    const header = '// Fallback TypeScript interface generated from JSON Schema';
    if (body.trim().startsWith('{')) {
      return `${header}\nexport interface ResponseBody ${body}`;
    }
    return `${header}\nexport type ResponseBody = ${body};`;
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


