import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
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
import { MonacoEditorModule, NgxEditorModel } from 'ngx-monaco-editor-v2';

import {SourceSystemEndpointResourceService} from '../../service/sourceSystemEndpointResource.service';
import {HttpClient} from '@angular/common/http';
import {SourceSystemResourceService} from '../../service/sourceSystemResource.service';

import {ApiEndpointQueryParamResourceService} from '../../service/apiEndpointQueryParamResource.service';
import {SourceSystemEndpointDTO} from '../../models/sourceSystemEndpointDTO';
import {ApiEndpointQueryParamDTO} from '../../models/apiEndpointQueryParamDTO';
import {ApiEndpointQueryParamType} from '../../models/apiEndpointQueryParamType';
import {CreateSourceSystemEndpointDTO} from '../../models/createSourceSystemEndpointDTO';

import { load as parseYAML } from 'js-yaml';
import { SourceSystemDTO } from '../../models/sourceSystemDTO';
import { ManageEndpointParamsComponent } from '../manage-endpoint-params/manage-endpoint-params.component';


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
    ManageEndpointParamsComponent,
    MonacoEditorModule,
  ],
  templateUrl: './manage-endpoints.component.html',
  styleUrls: ['./manage-endpoints.component.css']
})
export class ManageEndpointsComponent implements OnInit {
  /**
   * Expose QueryParamType enum to the template.
   */
  public ApiEndpointQueryParamType = ApiEndpointQueryParamType;
  /**
   * ID of the source system whose endpoints are managed.
   */
  @Input() sourceSystemId!: number;
  @Output() backStep = new EventEmitter<void>();
  @Output() finish = new EventEmitter<void>();

  /**
   * List of endpoints fetched from the backend.
   */
  endpoints: SourceSystemEndpointDTO[] = [];
  /**
   * Reactive form for creating new endpoints.
   */
  endpointForm!: FormGroup;
  /**
   * Indicator whether endpoints are currently loading.
   */
  loading = false;

  /**
   * Currently selected endpoint for detail management or editing.
   */
  selectedEndpoint: SourceSystemEndpointDTO | null = null;

  /**
   * Available HTTP methods for endpoints.
   */
  httpRequestTypes = [
    {label: 'GET', value: 'GET'},
    {label: 'POST', value: 'POST'},
    {label: 'PUT', value: 'PUT'},
    {label: 'DELETE', value: 'DELETE'}
  ];

  /**
   * Base API URL of the source system, used for importing endpoints.
   */
  apiUrl: string | null = null;
  /**
   * Flag indicating whether an import of endpoints is in progress.
   */
  importing = false;

  /**
   * Controls visibility of the edit endpoint dialog.
   */
  editDialog: boolean = false;
  /**
   * Endpoint currently being edited.
   */
  editingEndpoint: SourceSystemEndpointDTO | null = null;
  /**
   * Reactive form for editing an existing endpoint.
   */
  editForm!: FormGroup;
  jsonEditorOptions = { theme: 'vs-dark', language: 'json', automaticLayout: true };
  jsonError: string | null = null;
  editJsonError: string | null = null;

  requestBodyEditorEndpoint: SourceSystemEndpointDTO | null = null;
  requestBodyEditorModel: NgxEditorModel = { value: '', language: 'json' };
  requestBodyEditorError: string | null = null;


  /**
   * Injects FormBuilder, endpoint and source system services, and HttpClient.
   */
  constructor(
    private fb: FormBuilder,
    private endpointSvc: SourceSystemEndpointResourceService,
    private sourceSystemService: SourceSystemResourceService,
    private http: HttpClient,
    private queryParamSvc: ApiEndpointQueryParamResourceService,
  ) {
  }

  /**
   * Initialize forms and load endpoints and source system API URL.
   */
  ngOnInit(): void {
    this.endpointForm = this.fb.group({
      endpointPath: ['', Validators.required],
      httpRequestType: ['GET', Validators.required],
      requestBodySchema: ['']
    });

    this.editForm = this.fb.group({
      endpointPath: ['', Validators.required],
      httpRequestType: ['GET', Validators.required],
      requestBodySchema: ['']
    });

    this.loadSourceSystemAndSetApiUrl();
    this.loadEndpoints(); // Endpoints beim Initialisieren laden
  }

  private loadSourceSystemAndSetApiUrl(): void {
    this.sourceSystemService.apiConfigSourceSystemIdGet(this.sourceSystemId)
      .subscribe({
        next: (sourceSystem: SourceSystemDTO) => {
          console.log('Loaded source system:', sourceSystem);
          console.log('openApiSpec field:', sourceSystem.openApiSpec);
          console.log('openApiSpec type:', typeof sourceSystem.openApiSpec);
          
          if (sourceSystem.openApiSpec && typeof sourceSystem.openApiSpec === 'string') {
            if (sourceSystem.openApiSpec.startsWith('http')) {
              this.apiUrl = sourceSystem.openApiSpec.trim();
              console.log('✅ Found OpenAPI URL in source system:', this.apiUrl);
            } else {
              this.apiUrl = sourceSystem.apiUrl!;
            }
          } else {
            this.apiUrl = sourceSystem.apiUrl!;
            console.log('ℹ️ No OpenAPI spec found, using API URL:', this.apiUrl);
          }
        },
        error: (err: any) => {
          console.error('Failed to load source system:', err);
          this.apiUrl = 'https://petstore.swagger.io/v2';
        }
      });
  }
  /**
   * Load endpoints for the current source system from the backend.
   */
  loadEndpoints() {
    if (!this.sourceSystemId) return;
    this.loading = true;
    this.endpointSvc
      .apiConfigSourceSystemSourceSystemIdEndpointGet(this.sourceSystemId)
      .subscribe({
        next: (eps: SourceSystemEndpointDTO[]) => {
          this.endpoints = eps;
          this.loading = false;
        },
        error: (err: any) => {
          console.error(err);
          this.loading = false;
        }
      });
  }

  /**
   * Create a new endpoint using form data and refresh list upon success.
   */
  addEndpoint() {
    if (this.endpointForm.invalid) return;
    this.jsonError = null;
    const dto = this.endpointForm.value as CreateSourceSystemEndpointDTO;
    if (dto.requestBodySchema) {
      try {
        JSON.parse(dto.requestBodySchema);
      } catch (e) {
        this.jsonError = 'Request-Body-Schema ist kein valides JSON.';
        return;
      }
    }
    this.endpointSvc
      .apiConfigSourceSystemSourceSystemIdEndpointPost(this.sourceSystemId, [dto])
      .subscribe({
        next: () => {
          this.endpointForm.reset({ endpointPath: '', httpRequestType: 'GET' });
          this.loadEndpoints();
        },
        error: console.error
      });
  }

  /**
   * Delete an endpoint by its ID and remove it from the list.
   * @param id ID of the endpoint to delete.
   */
  deleteEndpoint(id: number) {
    this.endpointSvc
      .apiConfigSourceSystemEndpointIdDelete(id)
      .subscribe({
        next: () => this.endpoints = this.endpoints.filter(e => e.id !== id),
        error: console.error
      });
  }


  /**
   * Open the edit dialog pre-filled with endpoint data.
   * @param endpoint Endpoint to edit.
   */
  openEditDialog(endpoint: SourceSystemEndpointDTO) {
    console.log('Opening edit dialog for endpoint', endpoint);
    this.editingEndpoint = endpoint;
    this.editForm.patchValue({
      endpointPath: endpoint.endpointPath,
      httpRequestType: endpoint.httpRequestType,
      requestBodySchema: endpoint.requestBodySchema || ''
    });
    this.editDialog = true;
    this.editJsonError = null;
  }

  /**
   * Save changes made to the editing endpoint and refresh list.
   */
  saveEdit() {
    if (!this.editingEndpoint || this.editForm.invalid) {
      return;
    }
    console.log('saveEdit called, editingEndpoint:', this.editingEndpoint);
    this.editJsonError = null;
    const dto: SourceSystemEndpointDTO = {
      id: this.editingEndpoint.id!,
      sourceSystemId: this.sourceSystemId,
      endpointPath: this.editForm.value.endpointPath,
      httpRequestType: this.editForm.value.httpRequestType,
      requestBodySchema: this.editForm.value.requestBodySchema
    };
    console.log('saveEdit DTO to send:', dto);
    if (dto.requestBodySchema) {
      try {
        JSON.parse(dto.requestBodySchema);
      } catch (e) {
        this.editJsonError = 'Request-Body-Schema ist kein valides JSON.';
        return;
      }
    }
    this.endpointSvc
      .apiConfigSourceSystemEndpointIdPut(this.editingEndpoint.id!, dto, 'body')
      .subscribe({
        next: () => {
          console.log('saveEdit success for id', this.editingEndpoint?.id);
          this.editDialog = false;
          this.loadEndpoints();
        },
        error: err => {
          console.error('saveEdit error', err);
        }
      });
  }

  showRequestBodyEditor(endpoint: SourceSystemEndpointDTO) {
    this.requestBodyEditorEndpoint = endpoint;
    this.requestBodyEditorModel = {
      value: endpoint.requestBodySchema || '',
      language: 'json'
    };
    this.requestBodyEditorError = null;
  }

  closeRequestBodyEditor() {
    this.requestBodyEditorEndpoint = null;
    this.requestBodyEditorModel = { value: '', language: 'json' };
    this.requestBodyEditorError = null;
  }

  saveRequestBodySchema() {
    if (!this.requestBodyEditorEndpoint) return;
    this.requestBodyEditorError = null;
    // JSON-Validierung
    if (this.requestBodyEditorModel.value) {
      try {
        JSON.parse(this.requestBodyEditorModel.value);
      } catch (e) {
        this.requestBodyEditorError = 'Request-Body-Schema ist kein valides JSON.';
        return;
      }
    }
    const updated: SourceSystemEndpointDTO = {
      ...this.requestBodyEditorEndpoint,
      requestBodySchema: this.requestBodyEditorModel.value
    };
    this.endpointSvc.apiConfigSourceSystemEndpointIdPut(updated.id!, updated, 'body').subscribe({
      next: () => {
        // Update local list
        const idx = this.endpoints.findIndex(e => e.id === updated.id);
        if (idx !== -1) this.endpoints[idx] = { ...updated };
        this.closeRequestBodyEditor();
      },
      error: err => {
        this.requestBodyEditorError = 'Fehler beim Speichern.';
        console.error('Fehler beim Speichern des Request-Body-Schemas:', err);
      }
    });
  }

  onRequestBodyModelChange(event: any) {
    let value: string = '';
    if (typeof event === 'string') {
      value = event;
    } else if (event && typeof event.detail === 'string') {
      value = event.detail;
    } else if (event && event.target && typeof event.target.value === 'string') {
      value = event.target.value;
    } else {
      // Fallback: versuche event als string zu casten
      value = String(event);
    }
    this.requestBodyEditorModel = {
      ...this.requestBodyEditorModel,
      value
    };
  }

  /**
   * Navigate back to the previous wizard step.
   */
  onBack() {
    this.backStep.emit();
  }

  /**
   * Finish the wizard and emit completion event.
   */
  onFinish() {
    this.finish.emit();
  }

  async importEndpoints(): Promise<void> {
    if (!this.apiUrl) {
      console.error('No API URL available for import');
      return;
    }
    this.importing = true;
    try {
      await this.tryImportFromUrl();
    } catch (err) {
      console.error('Import failed:', err);
      this.importing = false;
    }
  }

  /**
   * Attempts to import the OpenAPI spec from the apiUrl, using direct and fallback URLs.
   */
  private async tryImportFromUrl(): Promise<void> {
    if (!this.apiUrl) {
      throw new Error('No API URL available for import');
    }
    if (
      this.apiUrl.includes('swagger.json') ||
      this.apiUrl.includes('openapi.json') ||
      this.apiUrl.endsWith('.yaml') ||
      this.apiUrl.endsWith('.yml')
    ) {
      console.log('Using direct OpenAPI URL:', this.apiUrl);
      try {
        const openApiSpec = await this.http.get(this.apiUrl).toPromise();
        console.log('✅ Loaded OpenAPI spec from direct URL');
        await this.processOpenApiSpec(openApiSpec);
        this.importing = false;
      } catch (err) {
        console.error('Failed to load OpenAPI spec from direct URL:', err);
        this.importing = false;
        throw err;
      }
    } else {
      try {
        const openApiSpec = await this.http.get(this.apiUrl + '/swagger.json').toPromise();
        console.log('✅ Loaded OpenAPI spec from /swagger.json');
        await this.processOpenApiSpec(openApiSpec);
      } catch (err) {
        console.log('ℹ️ /swagger.json not found, trying alternative URLs...');
        this.tryAlternativeOpenApiUrls();
      }
    }
  }


private tryAlternativeOpenApiUrls() {
  const alternativeUrls = [
    `${this.apiUrl}/v2/swagger.json`,
    `${this.apiUrl}/api/v3/openapi.json`,
    `${this.apiUrl}/swagger/v1/swagger.json`,
    `${this.apiUrl}/api-docs`,
    `${this.apiUrl}/openapi.json`,
    `${this.apiUrl}/docs/swagger.json`,
    `${this.apiUrl}/openapi.yaml`,   
    `${this.apiUrl}/openapi.yml`,     
    
  ];
  console.log('🔍 Trying alternative OpenAPI URLs...');
  this.loadOpenApiFromUrls(alternativeUrls, 0);
}


 


private loadOpenApiFromUrls(urls: string[], index: number) {
  if (index >= urls.length) {
    console.error('❌ Could not load OpenAPI spec from any URL');
    this.importing = false;
    return;
  }
  const url = urls[index];
  console.log(`⏳ Trying: ${url}`);

  const isJson = url.endsWith('.json');
  this.http.get(url, {
    responseType: isJson ? 'json' : 'text' as 'json'
  }).subscribe({
    next: (raw: any) => {
      console.log(`✅ SUCCESS! Loaded OpenAPI spec from: ${url}`);
      let spec: any;
      if (isJson) {
        spec = raw;
      } else {
        try {
          spec = parseYAML(raw as string);
        } catch (e) {
          console.error('Failed to parse YAML', e);
          this.loadOpenApiFromUrls(urls, index + 1);
          return;
        }
      }
      this.processOpenApiSpec(spec);
    },
    error: () => {
      console.log(`❌ Not found or invalid spec at: ${url}`);
      this.loadOpenApiFromUrls(urls, index + 1);
    }
  });
}




 private async processOpenApiSpec(spec: any): Promise<void> {
  
  try {
    alert('processOpenApiSpec start!')
    console.log('=== processOpenApiSpec called ===', spec);
    const endpointsToCreate: CreateSourceSystemEndpointDTO[] = [];
    const schemas = spec.components?.schemas || {};

    if (spec.paths) {
      for (const [path, pathItem] of Object.entries(spec.paths)) {
        console.log('Path:', path, pathItem);
        for (const [method, operation] of Object.entries(pathItem as any)) {
          console.log('  Method:', method, operation);
          if (['get', 'post', 'put', 'delete', 'patch', 'head', 'options'].includes(method.toLowerCase())) {
            const operationDetails = operation as any;
            
            const pathLevelParams = (pathItem as any).parameters || [];
            const operationLevelParams = operationDetails.parameters || [];
            const allParameters = [...pathLevelParams, ...operationLevelParams];
            
            const formattedPath = this.formatPathWithParameters(path, allParameters);
            
            if (['post', 'put'].includes(method.toLowerCase())) {
              console.log('    Found POST/PUT:', method, path);
            }

            // Request-Body-Schema extrahieren (nur für POST/PUT)
            let requestBodySchema: string | undefined = undefined;
            if (['post', 'put'].includes(method.toLowerCase())) {
              const requestBody = operationDetails.requestBody;
              if (requestBody?.content?.['application/json']?.schema) {
                console.log('      Found requestBody schema for', method, path, requestBody.content['application/json'].schema);
                let schema = requestBody.content['application/json'].schema;
                console.log('      Original schema for', method.toUpperCase(), path, ':', schema);
                schema = this.resolveRefs(schema, schemas);
                console.log('      Resolved schema for', method.toUpperCase(), path, ':', schema);
                if (schema) {
                  requestBodySchema = JSON.stringify(schema, null, 2);
                  console.log('      Final requestBodySchema for', method.toUpperCase(), path, ':', requestBodySchema);
                }
              }
            }

            const endpoint: CreateSourceSystemEndpointDTO = {
              endpointPath: formattedPath,
              httpRequestType: method.toUpperCase(),
              requestBodySchema
            };

            endpointsToCreate.push(endpoint);
          }
        }
      }
    }

    if (endpointsToCreate.length > 0) {
      // Zeige die importierten Endpunkte direkt im UI, bevor Backend-Reload
      this.endpoints = endpointsToCreate;
      this.endpointSvc.apiConfigSourceSystemSourceSystemIdEndpointPost(this.sourceSystemId, endpointsToCreate)
        .subscribe({
          next: () => {
            // this.loadEndpoints(); // Deaktiviert, damit die aufgelösten Endpunkte im UI bleiben
            console.log(`${endpointsToCreate.length} Endpoints erfolgreich importiert`);
          },
          error: (error) => {
            console.error('Fehler beim Speichern der Endpoints:', error);
          }
        });
    }
  } catch (error) {
    console.error('Fehler beim Verarbeiten der OpenAPI Spec:', error);
  } finally {
    this.importing = false;
  }
}

  /**
   * Parse OpenAPI specification and extract endpoints with their parameters
   */
  private parseOpenApiSpec(spec: any): Array<{endpoint: CreateSourceSystemEndpointDTO, pathParams: string[]}> {
    const endpoints: Array<{endpoint: CreateSourceSystemEndpointDTO, pathParams: string[]}> = [];
    
    if (!spec.paths) {
      console.warn('No paths found in OpenAPI specification');
      return endpoints;
    }

    
    Object.keys(spec.paths).forEach(path => {
      const pathItem = spec.paths[path];
      
      
      const httpMethods = ['get', 'post', 'put', 'delete', 'patch', 'options', 'head'];
      
      
      Object.keys(pathItem).forEach(method => {
        if (httpMethods.includes(method.toLowerCase())) {
          const operation = pathItem[method];
          
        
          const endpoint: CreateSourceSystemEndpointDTO = {
            endpointPath: path,
            httpRequestType: method.toUpperCase()
          };

          
          const pathParams = this.extractPathParameters(path);

          endpoints.push({
            endpoint,
            pathParams
          });
        }
      });
    });

    return endpoints;
  }


  /**
   * Utility: Ensure a parameter name is wrapped in curly braces {paramName}
   */
  private ensureBraces(paramName: string): string {
    const cleanName = paramName.replace(/[{}]/g, '');
    return `{${cleanName}}`;
  }

  /**
   * Extract path parameter names from an endpoint path.
   * Example: "/users/{userId}/posts/{postId}" -> ["{userId}", "{postId}"]
   * Jetzt: Gibt immer {paramName} zurück, auch wenn nur userId gefunden wird.
   */
  private extractPathParameters(endpointPath: string): string[] {
    // Suche nach {paramName}
    const pathParamRegex = /\{([^}]+)\}/g;
    const matches: string[] = [];
    let match: RegExpExecArray | null;
    while ((match = pathParamRegex.exec(endpointPath)) !== null) {
      matches.push(this.ensureBraces(match[1]));
    }
    // Suche nach :paramName, <paramName>, [paramName] und /:paramName
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
   * Save discovered endpoints to the backend
   */
  private saveDiscoveredEndpoints(discoveredEndpoints: Array<{endpoint: CreateSourceSystemEndpointDTO, pathParams: string[]}>) {
    if (discoveredEndpoints.length === 0) {
      console.warn('No endpoints discovered');
      this.importing = false;
      return;
    }

   
    const endpointDTOs = discoveredEndpoints.map(item => item.endpoint);
    this.endpointSvc
      .apiConfigSourceSystemSourceSystemIdEndpointPost(this.sourceSystemId, endpointDTOs)
      .subscribe({
        next: () => {
          console.log(`Successfully created ${endpointDTOs.length} endpoints`);
          this.loadEndpoints();
          setTimeout(() => {
          }, 1000);
        },
        error: (err) => {
          console.error('Failed to create endpoints:', err);
          this.importing = false;
        }
      });
  }

  /**
   * Validator to ensure path parameter format: starts with { and ends with } with at least one character inside.
   */
  private pathParamFormatValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const parent = control.parent;
      const val = control.value as string;
     
      if (typeof val === 'string' && val.match(/^\{[A-Za-z0-9_]+\}$/)) {
        return null;
      }
      return { invalidPathParamFormat: true };
    };
  }

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

  // Robuste rekursive Hilfsfunktion zum Auflösen von $ref in OpenAPI-Schemas
  private resolveRefs(schema: any, schemas: any, seen = new Set()): any {
    if (!schema) return schema;
    // $ref auflösen
    if (schema.$ref) {
      if (seen.has(schema.$ref)) return {}; // Zyklische Referenzen verhindern
      seen.add(schema.$ref);
      // $ref sieht aus wie "#/components/schemas/Pet"
      const refPath = schema.$ref.replace(/^#\//, '').split('/');
      // Wir erwarten, dass schemas = spec.components.schemas ist
      let resolved = schemas;
      // refPath: ["components", "schemas", "Pet"]
      for (const part of refPath.slice(2)) { // skip "components", "schemas"
        resolved = resolved?.[part];
      }
      if (!resolved) return {}; // Fallback falls nicht gefunden
      return this.resolveRefs(resolved, schemas, seen);
    }
    // Properties rekursiv auflösen
    if (schema.properties) {
      const newProps: any = {};
      for (const [key, value] of Object.entries(schema.properties)) {
        newProps[key] = this.resolveRefs(value, schemas, seen);
      }
      return { ...schema, properties: newProps };
    }
    // Items (für Arrays) rekursiv auflösen
    if (schema.items) {
      return { ...schema, items: this.resolveRefs(schema.items, schemas, seen) };
    }
    // allOf/anyOf/oneOf rekursiv auflösen
    for (const keyword of ['allOf', 'anyOf', 'oneOf']) {
      if (schema[keyword]) {
        return {
          ...schema,
          [keyword]: schema[keyword].map((s: any) => this.resolveRefs(s, schemas, seen))
        };
      }
    }
    // additionalProperties kann auch ein Schema sein
    if (typeof schema.additionalProperties === 'object' && schema.additionalProperties !== null) {
      return {
        ...schema,
        additionalProperties: this.resolveRefs(schema.additionalProperties, schemas, seen)
      };
    }
    return schema;
  }
}
