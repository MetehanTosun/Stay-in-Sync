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
import { MessageService } from 'primeng/api';
import { ValidatorFn, AbstractControl, ValidationErrors } from '@angular/forms';

import { TargetSystemEndpointResourceService } from '../../service/targetSystemEndpointResource.service';
import { TargetSystemEndpointDTO } from '../../models/targetSystemEndpointDTO';
import { CreateTargetSystemEndpointDTO } from '../../models/createTargetSystemEndpointDTO';
import { ManageEndpointParamsComponent } from '../../../source-system/components/manage-endpoint-params/manage-endpoint-params.component';
import { TabViewModule } from 'primeng/tabview';
import { MonacoEditorModule, NgxEditorModel } from 'ngx-monaco-editor-v2';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { Select } from 'primeng/select';
import { TargetSystemResourceService } from '../../service/targetSystemResource.service';
import { TargetSystemDTO } from '../../models/targetSystemDTO';
import { OpenApiImportService } from '../../../../core/services/openapi-import.service';
import { load as parseYAML } from 'js-yaml';
import { TargetResponsePreviewModalComponent } from '../response-preview-modal/response-preview-modal.component';
import { ConfirmationDialogComponent, ConfirmationDialogData } from '../../../source-system/components/confirmation-dialog/confirmation-dialog.component';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';

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
    TargetResponsePreviewModalComponent,
    ConfirmationDialogComponent,
    ToastModule,
    TooltipModule,
    Select
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
  httpRequestTypeOptions = [
    { label: 'GET', value: 'GET' },
    { label: 'POST', value: 'POST' },
    { label: 'PUT', value: 'PUT' },
    { label: 'DELETE', value: 'DELETE' },
    { label: 'PATCH', value: 'PATCH' },
  ];
  paramsDialog = false;
  selectedEndpointForParams: TargetSystemEndpointDTO | null = null;
  requestBodyDialog = false;
  requestBodyEditorEndpoint: TargetSystemEndpointDTO | null = null;
  requestBodyEditorError: string | null = null;
  responsePreviewDialog = false;
  selectedResponsePreviewEndpoint: TargetSystemEndpointDTO | null = null;
  
  // Confirmation dialog variables
  showConfirmationDialog = false;
  confirmationData: ConfirmationDialogData = {
    title: '',
    message: '',
    confirmLabel: 'Delete',
    cancelLabel: 'Cancel',
    severity: 'warning'
  };
  endpointToDelete: TargetSystemEndpointDTO | null = null;

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

  constructor(private api: TargetSystemEndpointResourceService, private fb: FormBuilder, private tsService: TargetSystemResourceService, private openapi: OpenApiImportService, private http: HttpClient, private messageService: MessageService) {}

  /**
   * Initializes the component, sets up the reactive form,
   * and triggers the initial endpoint loading.
   */
  ngOnInit(): void {
    this.form = this.fb.group({
      endpointPath: ['', [Validators.required, this.pathValidator()]],
      httpRequestType: ['GET', Validators.required],
      requestBodySchema: ['', this.jsonValidator()],
      responseBodySchema: ['', this.jsonValidator()]
    });
    this.load();

    this.form.get('responseBodySchema')?.valueChanges.subscribe(value => {
      this.generateTypeScriptForForm();
    });
  }

  /**
   * Loads all existing endpoints for the selected Target System.
   * Updates the table and loading state accordingly.
   */
  load(): void {
    if (!this.targetSystemId) return;
    this.loading = true;
    this.api.list(this.targetSystemId).subscribe({
      next: list => { this.endpoints = list; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  /**
   * Imports endpoint definitions from an OpenAPI specification.
   * Handles both remote (URL-based) and local (raw string) specifications.
   * Creates new endpoints and persists their parameters if available.
   */
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
        } catch (error) {
          console.error('[ManageTargetEndpoints] Error parsing local spec:', error);
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
      
      let createdList: any[] = [];
      if (toCreate.length) {
        const created = await firstValueFrom(this.api.create(this.targetSystemId, toCreate as any));
        createdList = (created as any[]) || [];
      }

      // Persist params for both newly created and already existing endpoints.
      // Duplicate params are skipped in persistParamsForEndpoint based on current backend state.
      const allEndpoints = [...createdList, ...existing];
      for (const ep of allEndpoints) {
        const key = `${ep.httpRequestType} ${ep.endpointPath}`;
        const params = paramsByKey[key] || [];
        if (ep.id && params.length) {
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
      this.load();
    } catch (error) {
      console.error('[ManageTargetEndpoints] Import failed:', error);
    } finally {
      this.importing = false;
    }
  }

  /**
   * Attempts to fetch possible OpenAPI specification files from common URLs.
   * @param baseUrl The base API URL to try candidate paths from.
   * @returns Parsed OpenAPI specification object if found.
   */
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

  /** Prepares the form for creating a new Target Endpoint. */
  openCreate(): void {
    this.editing = null;
    this.dialogTitle = 'New Endpoint';
    this.form.reset({ endpointPath: '', httpRequestType: 'GET', requestBodySchema: '', responseBodySchema: '' });
    this.showDialog = true;
  }

  /**
   * Creates a new endpoint from the form values.
   * Displays a success toast and reloads the endpoint list.
   */
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
        this.messageService.add({ key: 'endpoints', severity: 'success', summary: 'Endpoint Created', detail: 'Endpoint has been successfully created.', life: 3000 });
      },
      error: (error) => {
        console.error('[ManageTargetEndpoints] Error creating endpoint:', error);
      }
    });
  }

  /**
   * Opens the edit dialog and fills the form with the selected endpoint data.
   * @param row The endpoint to edit.
   */
  openEdit(row: TargetSystemEndpointDTO): void {
    this.editing = row;
    this.dialogTitle = 'Edit Endpoint';
    this.form.reset({ endpointPath: row.endpointPath, httpRequestType: row.httpRequestType, requestBodySchema: row.requestBodySchema || '', responseBodySchema: row.responseBodySchema || '' });
    this.showDialog = true;
  }

  /**
   * Saves endpoint changes. Updates an existing one or creates a new one if none is selected.
   * Displays success or error messages accordingly.
   */
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
      this.api.replace(this.editing.id, dto).subscribe({ 
        next: () => { 
          this.showDialog = false; 
          this.load(); 
          this.messageService.add({ key: 'endpoints', severity: 'success', summary: 'Endpoint Updated', detail: 'Endpoint has been successfully updated.', life: 3000 });
        },
        error: (error) => {
          console.error('[ManageTargetEndpoints] Error updating endpoint:', error);
        }
      });
    } else {
      const payload: CreateTargetSystemEndpointDTO = {
        endpointPath: this.form.value.endpointPath,
        httpRequestType: this.form.value.httpRequestType,
        // Backend DTO now supports request/response body on create
        // These are optional; send if present
        ...(this.form.value.requestBodySchema ? { requestBodySchema: this.form.value.requestBodySchema } : {}),
        ...(this.form.value.responseBodySchema ? { responseBodySchema: this.form.value.responseBodySchema } : {})
      } as any;
      this.api.create(this.targetSystemId, [payload]).subscribe({ 
        next: () => { 
          this.showDialog = false; 
          this.load(); 
          this.messageService.add({ key: 'endpoints', severity: 'success', summary: 'Endpoint Created', detail: 'Endpoint has been successfully created.', life: 3000 });
        },
        error: (error) => {
          console.error('[ManageTargetEndpoints] Error creating endpoint:', error);
        }
      });
    }
  }

  /**
   * Opens a confirmation dialog before deleting the selected endpoint.
   * @param row The endpoint to delete.
   */
  delete(row: TargetSystemEndpointDTO): void {
    this.endpointToDelete = row;
    this.confirmationData = {
      title: 'Delete Endpoint',
      message: `Are you sure you want to delete the endpoint "${row.endpointPath}" (${row.httpRequestType})? This action cannot be undone and will also delete all associated query parameters.`,
      confirmLabel: 'Delete',
      cancelLabel: 'Cancel',
      severity: 'warning'
    };
    this.showConfirmationDialog = true;
  }

  /**
   * Executes endpoint deletion after user confirmation.
   * Updates the list and shows a success message upon completion.
   */
  onConfirmationConfirmed(): void {
    if (this.endpointToDelete && this.endpointToDelete.id) {
      this.api.delete(this.endpointToDelete.id).subscribe({ 
        next: () => {
          this.load();
          this.messageService.add({ key: 'endpoints', severity: 'success', summary: 'Endpoint Deleted', detail: 'Endpoint has been successfully deleted.', life: 3000 });
          this.endpointToDelete = null;
        },
        error: (error) => {
          console.error('[ManageTargetEndpoints] Error deleting endpoint:', error);
          this.endpointToDelete = null;
        }
      });
    }
  }

  onConfirmationCancelled(): void {
    this.endpointToDelete = null;
  }

  /** Opens the parameters dialog for the selected endpoint. */
  openParams(row: TargetSystemEndpointDTO): void {
    this.selectedEndpointForParams = row;
    this.paramsDialog = true;
  }

  /**
   * Opens a draggable overlay editor for editing the endpoint's request body schema.
   * @param row The endpoint whose request body should be edited.
   */
  openRequestBodyEditor(row: TargetSystemEndpointDTO): void {
    this.requestBodyEditorEndpoint = row;
    this.requestBodyEditorModel = { value: row.requestBodySchema || '// No request body schema', language: 'json' };
    this.requestBodyEditorError = null;
  }

  /** Closes the request body schema editor overlay. */
  closeRequestBodyEditor(): void {
    this.requestBodyEditorEndpoint = null;
    this.requestBodyEditorModel = { value: '', language: 'json' };
    this.requestBodyEditorError = null;
  }

  /**
   * Handles live updates from the Monaco editor when request body schema changes.
   * @param event Editor change event containing the updated JSON.
   */
  onRequestBodyModelChange(event: any): void {
    const value = typeof event === 'string' ? event : (event?.detail ?? event?.target?.value ?? String(event));
    this.requestBodyEditorModel = { ...this.requestBodyEditorModel, value };
  }

  /**
   * Opens a modal dialog to preview the generated TypeScript for a given endpoint response.
   * @param row The endpoint whose response should be previewed.
   */
  openResponsePreview(row: TargetSystemEndpointDTO): void {
    this.selectedResponsePreviewEndpoint = row;
    this.responsePreviewDialog = true;
  }

  /**
   * Generates a TypeScript interface from the JSON response schema
   * currently entered in the form.
   */
  generateTypeScriptForForm(): void {
    const schema = this.form.value.responseBodySchema;
    if (!schema) { this.typescriptModel = { value: '// No schema provided', language: 'typescript' }; return; }
    this.api.generateTypeScript(this.editing?.id || 0, { jsonSchema: schema }).subscribe({
      next: (resp) => this.typescriptModel = { value: resp.generatedTypeScript || '', language: 'typescript' }
    });
  }

  /**
   * Generates TypeScript model preview for a selected endpoint response body.
   */
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

  /**
   * Triggers TypeScript generation when switching to the TypeScript tab in the dialog.
   * @param event Tab change event.
   */
  onDialogTabChange(event: any): void {
    if (event?.index === 1) {
      this.generateTypeScriptForForm();
    }
  }

  /**
   * Triggers TypeScript regeneration for preview when switching tabs in the preview modal.
   * @param event Tab change event.
   */
  onResponseTabChange(event: any): void {
    if (event?.index === 1) {
      this.generateTypeScriptForPreview();
    }
  }

  /**
   * Custom validator to ensure endpoint paths start with "/" and have valid parameter syntax.
   * @returns Validator function.
   */
  private pathValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const value = control.value;
      if (!value) return null;

      // Path should start with /
      if (!value.startsWith('/')) {
        return { pathFormat: { message: 'Path must start with /' } };
      }

      // Check for valid path parameter format {param}
      const pathParamRegex = /\{[a-zA-Z_][a-zA-Z0-9_]*\}/g;
      const invalidParams = value.match(/\{[^}]*\}/g)?.filter((param: string) => 
        !pathParamRegex.test(param)
      );

      if (invalidParams && invalidParams.length > 0) {
        return { pathFormat: { message: 'Invalid path parameter format. Use {paramName}' } };
      }

      return null;
    };
  }

  /**
   * Custom validator to verify that provided JSON strings are valid objects.
   * @returns Validator function.
   */
  private jsonValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const value = control.value;
      if (!value || value.trim() === '') return null;

      try {
        const parsed = JSON.parse(value);
        if (typeof parsed !== 'object' || parsed === null) {
          return { jsonFormat: { message: 'JSON must be an object' } };
        }
        return null;
      } catch (error) {
        return { jsonFormat: { message: 'Invalid JSON format' } };
      }
    };
  }

  /**
   * Builds tooltip text describing validation issues preventing endpoint creation.
   * @returns Combined tooltip message or empty string if form is valid.
   */
  getAddEndpointTooltip(): string {
    if (this.form.valid) {
      return '';
    }

    const errors: string[] = [];
    
    const pathControl = this.form.get('endpointPath');
    if (pathControl?.invalid) {
      if (pathControl.errors?.['required']) {
        errors.push('Path is required');
      } else if (pathControl.errors?.['pathFormat']) {
        errors.push('Path must start with /');
      }
    }

    const requestBodyControl = this.form.get('requestBodySchema');
    if (requestBodyControl?.invalid && requestBodyControl.errors?.['jsonFormat']) {
      errors.push('Invalid JSON in Request Body Schema');
    }

    const responseBodyControl = this.form.get('responseBodySchema');
    if (responseBodyControl?.invalid && responseBodyControl.errors?.['jsonFormat']) {
      errors.push('Invalid JSON in Response Body Schema');
    }

    return errors.length > 0 ? errors.join(', ') : '';
  }

  /**
   * Builds tooltip text for the Save button (alias for getAddEndpointTooltip)
   * @returns Combined tooltip message or empty string if form is valid.
   */
  getSaveTooltip(): string {
    return this.getAddEndpointTooltip();
  }

}
