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
import { TargetSystemResourceService } from '../../service/targetSystemResource.service';
import { TargetSystemDTO } from '../../models/targetSystemDTO';
import { OpenApiImportService } from '../../../../core/services/openapi-import.service';
import { load as parseYAML } from 'js-yaml';

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
    MonacoEditorModule
  ],
  template: `
    <p-card header="Target Endpoints">
      <button pButton label="New" icon="pi pi-plus" (click)="openCreate()"></button>
      <div class="p-mb-3">
        <button pButton type="button" icon="pi pi-cloud-download" label="Import Endpoints" [disabled]="importing" (click)="importEndpoints()"></button>
        <p-progressSpinner *ngIf="importing" styleClass="p-ml-2" strokeWidth="4"></p-progressSpinner>
      </div>
      <p-table [value]="endpoints" [loading]="loading" class="mt-3">
        <ng-template pTemplate="header">
          <tr>
            <th>Path</th>
            <th>Method</th>
            <th>Request Body</th>
            <th>Response Body</th>
            <th>Actions</th>
          </tr>
        </ng-template>
        <ng-template pTemplate="body" let-row>
          <tr>
            <td>{{ row.endpointPath }}</td>
            <td>{{ row.httpRequestType }}</td>
            <td>
              <button *ngIf="row.httpRequestType === 'POST' || row.httpRequestType === 'PUT'" pButton type="button" label="Request Body" (click)="openRequestBodyEditor(row)"></button>
            </td>
            <td>
              <button pButton type="button" label="Response Preview" (click)="openResponsePreview(row)"></button>
            </td>
            <td>
              <button pButton icon="pi pi-pencil" class="p-button-text" (click)="openEdit(row)"></button>
              <button pButton icon="pi pi-trash" class="p-button-text p-button-danger" (click)="delete(row)"></button>
              <button pButton icon="pi pi-sliders-h" class="p-button-text" (click)="openParams(row)"></button>
            </td>
          </tr>
        </ng-template>
      </p-table>
    </p-card>

    <p-dialog [(visible)]="showDialog" [modal]="true" [style]="{width: '60vw', height: '80vh'}" [header]="dialogTitle">
      <form [formGroup]="form" class="p-fluid">
        <div class="p-field">
          <label for="endpointPath">Endpoint Path</label>
          <input id="endpointPath" pInputText formControlName="endpointPath">
        </div>
        <div class="p-field">
          <label for="httpRequestType">HTTP Method</label>
          <select id="httpRequestType" formControlName="httpRequestType">
            <option *ngFor="let m of httpRequestTypes" [value]="m">{{ m }}</option>
          </select>
        </div>
        <div class="p-field" *ngIf="form.get('httpRequestType')?.value === 'POST' || form.get('httpRequestType')?.value === 'PUT'">
          <label>Request Body Schema (JSON)</label>
          <ngx-monaco-editor [options]="jsonEditorOptions" formControlName="requestBodySchema"></ngx-monaco-editor>
        </div>
        <div class="p-field">
          <label>Response Body Schema</label>
          <p-tabView (onChange)="onDialogTabChange($event)">
            <p-tabPanel header="JSON">
              <ngx-monaco-editor [options]="jsonEditorOptions" formControlName="responseBodySchema"></ngx-monaco-editor>
            </p-tabPanel>
            <p-tabPanel header="TypeScript">
              <ngx-monaco-editor [options]="typescriptEditorOptions" [model]="typescriptModel"></ngx-monaco-editor>
            </p-tabPanel>
          </p-tabView>
        </div>
      </form>
      <ng-template pTemplate="footer">
        <button pButton label="Cancel" class="p-button-text" (click)="showDialog=false"></button>
        <button pButton label="Save" (click)="save()" [disabled]="form.invalid"></button>
      </ng-template>
    </p-dialog>

    <p-dialog [(visible)]="paramsDialog" [modal]="true" [style]="{width: '720px'}" header="Endpoint Parameters">
      <app-manage-endpoint-params *ngIf="selectedEndpointForParams" [endpointId]="selectedEndpointForParams.id!" [endpointPath]="selectedEndpointForParams.endpointPath"></app-manage-endpoint-params>
    </p-dialog>

    <p-dialog [(visible)]="requestBodyDialog" [modal]="true" [style]="{width: '60vw', height: '70vh'}" header="Request Body Schema">
      <ngx-monaco-editor [options]="jsonEditorOptions" [model]="requestBodyEditorModel"></ngx-monaco-editor>
      <ng-template pTemplate="footer">
        <button pButton label="Close" class="p-button-text" (click)="requestBodyDialog=false"></button>
      </ng-template>
    </p-dialog>

    <p-dialog [(visible)]="responsePreviewDialog" [modal]="true" [style]="{width: '60vw', height: '70vh'}" header="Response Body">
      <p-tabView (onChange)="onResponseTabChange($event)">
        <p-tabPanel header="JSON">
          <ngx-monaco-editor [options]="jsonEditorOptions" [model]="responseJsonModel"></ngx-monaco-editor>
        </p-tabPanel>
        <p-tabPanel header="TypeScript">
          <ngx-monaco-editor [options]="typescriptEditorOptions" [model]="responseTypeScriptModel"></ngx-monaco-editor>
        </p-tabPanel>
      </p-tabView>
      <ng-template pTemplate="footer">
        <button pButton label="Close" class="p-button-text" (click)="responsePreviewDialog=false"></button>
      </ng-template>
    </p-dialog>
  `,
  styles: [``]
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
  responsePreviewDialog = false;

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
      const apiUrl = (ts && (ts as any).openAPI && (ts as any).openAPI.startsWith('http')) ? (ts as any).openAPI.trim() : (ts.apiUrl || '');
      if (!apiUrl) { this.importing = false; return; }
      // discover endpoints and params via shared service
      const endpoints = await this.openapi.discoverEndpointsFromSpecUrl(apiUrl);
      const spec = await this.loadSpecCandidates(apiUrl);
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
    this.showDialog = true;
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
    this.requestBodyEditorModel = { value: row.requestBodySchema || '// No request body schema', language: 'json' };
    this.requestBodyDialog = true;
  }

  openResponsePreview(row: TargetSystemEndpointDTO): void {
    this.responseJsonModel = { value: row.responseBodySchema || '// No response body schema', language: 'json' };
    this.responseTypeScriptModel = { value: row.responseDts || '// Click Generate to build TypeScript' , language: 'typescript'};
    this.responsePreviewDialog = true;
  }

  generateTypeScriptForForm(): void {
    const schema = this.form.value.responseBodySchema;
    if (!schema) { this.typescriptModel = { value: '// No schema provided', language: 'typescript' }; return; }
    this.api.generateTypeScript(this.editing?.id || 0, { jsonSchema: schema }).subscribe({
      next: resp => this.typescriptModel = { value: resp.generatedTypeScript || '', language: 'typescript' }
    });
  }

  generateTypeScriptForPreview(): void {
    const schema = this.responseJsonModel.value as string;
    if (!schema) { return; }
    // in preview we need a valid endpoint id
    const endpoint = this.selectedEndpointForParams ?? this.endpoints[0];
    const endpointId = endpoint?.id || 0;
    this.api.generateTypeScript(endpointId, { jsonSchema: schema }).subscribe({
      next: resp => this.responseTypeScriptModel = { value: resp.generatedTypeScript || '', language: 'typescript' }
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


