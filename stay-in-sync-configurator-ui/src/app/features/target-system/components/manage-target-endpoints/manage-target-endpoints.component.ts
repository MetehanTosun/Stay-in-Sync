import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { CardModule } from 'primeng/card';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { TargetSystemEndpointResourceService } from '../../service/targetSystemEndpointResource.service';
import { TargetSystemEndpointDTO } from '../../models/targetSystemEndpointDTO';
import { CreateTargetSystemEndpointDTO } from '../../models/createTargetSystemEndpointDTO';
import { ManageEndpointParamsComponent } from '../../../source-system/components/manage-endpoint-params/manage-endpoint-params.component';
import { TabViewModule } from 'primeng/tabview';
import { MonacoEditorModule, NgxEditorModel } from 'ngx-monaco-editor-v2';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { TargetSystemResourceService } from '../../service/targetSystemResource.service';
import { TargetSystemDTO } from '../../models/targetSystemDTO';
import { OpenApiImportService } from '../../../../core/services/openapi-import.service';
import { load as parseYAML } from 'js-yaml';
import { TargetResponsePreviewModalComponent } from '../response-preview-modal/response-preview-modal.component';

@Component({
  standalone: true,
  selector: 'app-manage-target-endpoints',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TableModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    DropdownModule,
    CardModule,
    ProgressSpinnerModule,
    ManageEndpointParamsComponent,
    TabViewModule,
    MonacoEditorModule,
    DragDropModule,
    TargetResponsePreviewModalComponent
  ],
  templateUrl: './manage-target-endpoints.component.html',
  styles: [`
    .request-body-editor-overlay {
      position: fixed;
      top: 10vh;
      left: 10vw;
      width: 60vw;
      height: 70vh;
      background: var(--surface-card);
      border: 1px solid var(--surface-border);
      border-radius: 8px;
      box-shadow: 0 2px 12px rgba(0,0,0,0.2);
      z-index: 1000;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }
    .request-body-editor-overlay .editor-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: .75rem 1rem;
      background: var(--surface-ground);
      border-bottom: 1px solid var(--surface-border);
      cursor: move;
      user-select: none;
    }
    .request-body-editor-overlay .close-btn {
      border: none;
      background: transparent;
      font-size: 1rem;
      cursor: pointer;
    }
    .request-body-editor-overlay ngx-monaco-editor {
      flex: 1;
      min-height: 0;
    }
    .request-body-editor-overlay .editor-footer {
      padding: .5rem 1rem;
      border-top: 1px solid var(--surface-border);
      background: var(--surface-ground);
    }
  `]
})
export class ManageTargetEndpointsComponent implements OnInit {
  @Input() targetSystemId!: number;
  @Output() finish = new EventEmitter<void>();

  endpoints: TargetSystemEndpointDTO[] = [];
  loading = false;
  importing = false;
  showDialog = false;
  dialogTitle = 'Neuer Endpoint';
  form!: FormGroup;
  editing: TargetSystemEndpointDTO | null = null;
  httpRequestTypes: Array<'GET'|'POST'|'PUT'|'DELETE'|'PATCH'> = ['GET','POST','PUT','DELETE','PATCH'];
  paramsDialog = false;
  selectedEndpointForParams: TargetSystemEndpointDTO | null = null;
  requestBodyDialog = false;
  requestBodyEditorEndpoint: TargetSystemEndpointDTO | null = null;
  requestBodyEditorError: string | null = null;
  responsePreviewDialog = false;
  selectedResponsePreviewEndpoint: TargetSystemEndpointDTO | null = null;

  jsonEditorOptions = {
    theme: 'vs-dark',
    language: 'json',
    automaticLayout: true,
    minimap: { enabled: false }
  };
  typescriptEditorOptions = {
    theme: 'vs-dark',
    language: 'typescript',
    automaticLayout: true,
    readOnly: true,
    minimap: { enabled: false }
  };
  typescriptModel: NgxEditorModel = { value: '', language: 'typescript' };
  requestBodyEditorModel: NgxEditorModel = { value: '', language: 'json' };
  responseJsonModel: NgxEditorModel = { value: '', language: 'json' };
  responseTypeScriptModel: NgxEditorModel = { value: '', language: 'typescript' };

  constructor(private api: TargetSystemEndpointResourceService, private fb: FormBuilder, private tsService: TargetSystemResourceService, private openapi: OpenApiImportService, private http: HttpClient) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      endpointPath: ['', Validators.required],
      httpRequestType: ['GET', Validators.required],
      requestBodySchema: [''],
      responseBodySchema: ['']
    });
    this.load();

    // Auto-generate TypeScript when responseBodySchema changes (like Source Systems)
    this.form.get('responseBodySchema')?.valueChanges.subscribe(value => {
      this.generateTypeScriptForForm();
    });
  }

  load(): void {
    if (!this.targetSystemId) return;
    this.loading = true;
    this.api.list(this.targetSystemId).subscribe({
      next: list => { this.endpoints = list; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  // -------- Import from OpenAPI (similar to Source Systems) --------
  async importEndpoints(): Promise<void> {
    this.importing = true;
    try {
      // Load target-system to decide where to fetch OpenAPI spec
      const ts = await firstValueFrom(this.tsService.getById(this.targetSystemId));

      let endpoints: any[] = [];
      let spec: any | null = null;

      // 1) If openAPI is provided and is raw content (uploaded file), parse locally
      if ((ts as any).openAPI && typeof (ts as any).openAPI === 'string' && !(ts as any).openAPI.startsWith('http')) {
        try {
          try {
            spec = JSON.parse((ts as any).openAPI as string);
          } catch {
            spec = parseYAML((ts as any).openAPI as string);
          }
          endpoints = this.openapi.discoverEndpointsFromSpec(spec);
        } catch {
          spec = null;
          endpoints = [];
        }
      }

      // 2) Fallback: Use URL (either openAPI URL or apiUrl base + candidates)
      const apiUrl = ((ts as any).openAPI && (ts as any).openAPI.startsWith('http')) ? (ts as any).openAPI.trim() : (ts.apiUrl || '');
      if ((!spec || endpoints.length === 0) && apiUrl) {
        endpoints = await this.openapi.discoverEndpointsFromSpecUrl(apiUrl);
        spec = await this.loadSpecCandidates(apiUrl);
      }

      const paramsByKey = spec ? this.openapi.discoverParamsFromSpec(spec) : {};
      // filter out duplicates by METHOD + PATH (matches backend unique key)
      const existing = await firstValueFrom(this.api.list(this.targetSystemId));
      const existingKeys = new Set(existing.map(e => `${e.httpRequestType} ${e.endpointPath}`));
      const seenNew = new Set<string>();
      const toCreate = endpoints.filter((e: any) => {
        const key = `${e.httpRequestType} ${e.endpointPath}`;
        if (existingKeys.has(key) || seenNew.has(key)) return false;
        seenNew.add(key);
        return true;
      });
      if (toCreate.length) {
        const created = await firstValueFrom(this.api.create(this.targetSystemId, toCreate as any));
        // Map created endpoints to keys and persist params
        for (const ep of (created as any[])) {
          const key = `${ep.httpRequestType} ${ep.endpointPath}`;
          const params = paramsByKey[key] || [];
          if (ep.id && params.length) await this.openapi.persistParamsForEndpoint(ep.id, params);
        }
        this.load();
      }
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

  openCreate(): void {
    this.editing = null;
    this.dialogTitle = 'New Endpoint';
    this.form.reset({ endpointPath: '', httpRequestType: 'GET', requestBodySchema: '', responseBodySchema: '' });
    // no dialog; inline create form is visible by default
  }

  addEndpoint(): void {
    if (this.form.invalid) return;
    const payload: CreateTargetSystemEndpointDTO = {
      endpointPath: this.form.value.endpointPath,
      httpRequestType: this.form.value.httpRequestType,
      ...(this.form.value.requestBodySchema ? { requestBodySchema: this.form.value.requestBodySchema } : {}),
      ...(this.form.value.responseBodySchema ? { responseBodySchema: this.form.value.responseBodySchema } : {})
    } as any;
    this.api.create(this.targetSystemId, [payload]).subscribe({
      next: () => {
        this.form.reset({ endpointPath: '', httpRequestType: 'GET', requestBodySchema: '', responseBodySchema: '' });
        this.load();
      }
    });
  }

  openEdit(row: TargetSystemEndpointDTO): void {
    this.editing = row;
    this.dialogTitle = 'Edit Endpoint';
    this.form.reset({ endpointPath: row.endpointPath, httpRequestType: row.httpRequestType, requestBodySchema: row.requestBodySchema || '', responseBodySchema: row.responseBodySchema || '' });
    this.showDialog = true;
  }

  save(): void {
    if (this.editing?.id) {
      const dto: TargetSystemEndpointDTO = {
        id: this.editing.id,
        targetSystemId: this.targetSystemId,
        endpointPath: this.form.value.endpointPath,
        httpRequestType: this.form.value.httpRequestType,
        requestBodySchema: this.form.value.requestBodySchema,
        responseBodySchema: this.form.value.responseBodySchema
      };
      this.api.replace(this.editing.id, dto).subscribe({ next: () => { this.showDialog = false; this.load(); } });
    } else {
      const payload: CreateTargetSystemEndpointDTO = {
        endpointPath: this.form.value.endpointPath,
        httpRequestType: this.form.value.httpRequestType,
        // Backend DTO now supports request/response body on create
        // These are optional; send if present
        ...(this.form.value.requestBodySchema ? { requestBodySchema: this.form.value.requestBodySchema } : {}),
        ...(this.form.value.responseBodySchema ? { responseBodySchema: this.form.value.responseBodySchema } : {})
      } as any;
      this.api.create(this.targetSystemId, [payload]).subscribe({ next: () => { this.showDialog = false; this.load(); } });
    }
  }

  delete(row: TargetSystemEndpointDTO): void {
    if (!row.id) return;
    this.api.delete(row.id).subscribe({ next: () => this.load() });
  }

  openParams(row: TargetSystemEndpointDTO): void {
    this.selectedEndpointForParams = row;
    this.paramsDialog = true;
  }

  openRequestBodyEditor(row: TargetSystemEndpointDTO): void {
    this.requestBodyEditorEndpoint = row;
    this.requestBodyEditorModel = { value: row.requestBodySchema || '// No request body schema', language: 'json' };
    this.requestBodyEditorError = null;
  }

  closeRequestBodyEditor(): void {
    this.requestBodyEditorEndpoint = null;
    this.requestBodyEditorModel = { value: '', language: 'json' };
    this.requestBodyEditorError = null;
  }

  onRequestBodyModelChange(event: any): void {
    const value = typeof event === 'string' ? event : (event?.detail ?? event?.target?.value ?? String(event));
    this.requestBodyEditorModel = { ...this.requestBodyEditorModel, value };
  }

  openResponsePreview(row: TargetSystemEndpointDTO): void {
    this.selectedResponsePreviewEndpoint = row;
    this.responsePreviewDialog = true;
  }

  generateTypeScriptForForm(): void {
    const schema = this.form.value.responseBodySchema;
    if (!schema) { this.typescriptModel = { value: '// No schema provided', language: 'typescript' }; return; }
    this.api.generateTypeScript(this.editing?.id || 0, { jsonSchema: schema }).subscribe({
      next: (resp) => this.typescriptModel = { value: resp.generatedTypeScript || '', language: 'typescript' }
    });
  }

  generateTypeScriptForPreview(): void {
    const schema = this.responseJsonModel.value as string;
    if (!schema) { return; }
    // in preview we need a valid endpoint id
    const endpoint = this.selectedEndpointForParams ?? this.endpoints[0];
    const endpointId = endpoint?.id || 0;
    this.api.generateTypeScript(endpointId, { jsonSchema: schema }).subscribe({
      next: (resp) => this.responseTypeScriptModel = { value: resp.generatedTypeScript || '', language: 'typescript' }
    });
  }

  onDialogTabChange(event: any): void {
    if (event?.index === 1) {
      this.generateTypeScriptForForm();
    }
  }

  onResponseTabChange(event: any): void {
    if (event?.index === 1) {
      this.generateTypeScriptForPreview();
    }
  }
}


