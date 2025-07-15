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

import {SourceSystemEndpointResourceService} from '../../service/sourceSystemEndpointResource.service';
import {HttpClient} from '@angular/common/http';
import {SourceSystemResourceService} from '../../service/sourceSystemResource.service';

import {ApiEndpointQueryParamResourceService} from '../../service/apiEndpointQueryParamResource.service';
import {SourceSystemEndpointDTO} from '../../models/sourceSystemEndpointDTO';
import {ApiEndpointQueryParamDTO} from '../../models/apiEndpointQueryParamDTO';
import {ApiEndpointQueryParamType} from '../../models/apiEndpointQueryParamType';
import {CreateSourceSystemEndpointDTO} from '../../models/createSourceSystemEndpointDTO';

import { load as parseYAML } from 'js-yaml';


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
    ProgressSpinnerModule
  ],
  templateUrl: './manage-endpoints.component.html',
  styleUrls: ['./manage-endpoints.component.css']
})
export class ManageEndpointsComponent implements OnInit {
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

  /**
   * Query Parameters for the selected endpoint.
   */
  queryParams: ApiEndpointQueryParamDTO[] = [];
  /**
   * Reactive form for creating new query parameters.
   */
  queryParamForm!: FormGroup;
  /**
   * Indicator whether query parameters are currently loading.
   */
  queryParamsLoading = false;
  /**
   * Available types for query parameters.
   */
  paramTypes = [
    {label: 'Query', value: ApiEndpointQueryParamType.Query},
    {label: 'Path', value: ApiEndpointQueryParamType.Path}
  ];


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
      httpRequestType: ['GET', Validators.required]
    });

    this.queryParamForm = this.fb.group({
      paramName: ['', [Validators.required, this.pathParamFormatValidator()]],
      queryParamType: [ApiEndpointQueryParamType.Query, Validators.required]
    });

    this.loadEndpoints();
    this.sourceSystemService
      .apiConfigSourceSystemIdGet(this.sourceSystemId, 'body')
      .subscribe(ss => this.apiUrl = ss.apiUrl);

    this.editForm = this.fb.group({
      endpointPath: ['', Validators.required],
      httpRequestType: ['GET', Validators.required]
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
    const dto = this.endpointForm.value as CreateSourceSystemEndpointDTO;
    this.endpointSvc
      .apiConfigSourceSystemSourceSystemIdEndpointPost(this.sourceSystemId, [dto])
      .subscribe({
        next: () => {
          this.endpointForm.reset({httpRequestType: 'GET'});
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
      httpRequestType: endpoint.httpRequestType
    });
    this.editDialog = true;
  }

  /**
   * Save changes made to the editing endpoint and refresh list.
   */
  saveEdit() {
    if (!this.editingEndpoint || this.editForm.invalid) {
      return;
    }
    console.log('saveEdit called, editingEndpoint:', this.editingEndpoint);
    const dto: SourceSystemEndpointDTO = {
      id: this.editingEndpoint.id!,
      sourceSystemId: this.sourceSystemId,
      endpointPath: this.editForm.value.endpointPath,
      httpRequestType: this.editForm.value.httpRequestType
    };
    console.log('saveEdit DTO to send:', dto);
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

// ...existing code...
importEndpoints() {
  if (!this.apiUrl) return;
  
  this.importing = true;
  
  // Versuche zuerst die Standard-URL
  this.http.get(this.apiUrl + '/swagger.json')
    .subscribe({
      next: (openApiSpec: any) => {
        console.log('✅ Loaded OpenAPI spec from /swagger.json');
        this.processOpenApiSpec(openApiSpec);
      },
      error: (err) => {
        console.log('ℹ️ /swagger.json not found, trying alternative URLs...');
        // Fallback: Versuche alternative URLs
        this.tryAlternativeOpenApiUrls();
      }
    });
}

private tryAlternativeOpenApiUrls() {
  const alternativeUrls = [
    `${this.apiUrl}/v2/swagger.json`,
    `${this.apiUrl}/api/v3/openapi.json`,
    `${this.apiUrl}/swagger/v1/swagger.json`,
    `${this.apiUrl}/api-docs`,
    `${this.apiUrl}/openapi.json`,
    `${this.apiUrl}/docs/swagger.json`,
    `${this.apiUrl}/openapi.yaml`,   // neu
    `${this.apiUrl}/openapi.yml`     // neu
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

  // Bestimme, ob wir als JSON oder als text holen müssen
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
        // parse YAML → JS-Objekt
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

/**
// ...existing code...

  /**
   * Process the loaded OpenAPI specification
   */
  private processOpenApiSpec(openApiSpec: any) {
    const discoveredEndpoints = this.parseOpenApiSpec(openApiSpec);
    console.log('Discovered endpoints:', discoveredEndpoints);
    this.saveDiscoveredEndpoints(discoveredEndpoints);
  }

  /**
// ...existing code...
  /**
   * Parse OpenAPI specification and extract endpoints with their parameters
   */
  private parseOpenApiSpec(spec: any): Array<{endpoint: CreateSourceSystemEndpointDTO, pathParams: string[], queryParams: string[]}> {
    const endpoints: Array<{endpoint: CreateSourceSystemEndpointDTO, pathParams: string[], queryParams: string[]}> = [];
    
    if (!spec.paths) {
      console.warn('No paths found in OpenAPI specification');
      return endpoints;
    }

    // Iteriere über alle Pfade in der OpenAPI-Spezifikation
    Object.keys(spec.paths).forEach(path => {
      const pathItem = spec.paths[path];
      
      // Ignoriere Metadaten-Felder in pathItem
      const httpMethods = ['get', 'post', 'put', 'delete', 'patch', 'options', 'head'];
      
      // Iteriere über alle HTTP-Methoden für diesen Pfad
      Object.keys(pathItem).forEach(method => {
        if (httpMethods.includes(method.toLowerCase())) {
          const operation = pathItem[method];
          
          // Erstelle Endpunkt-DTO
          const endpoint: CreateSourceSystemEndpointDTO = {
            endpointPath: path,
            httpRequestType: method.toUpperCase()
          };

          // Extrahiere Pfad-Parameter aus dem Pfad selbst (z.B. /users/{id})
          const pathParams = this.extractPathParameters(path);
          
          // Extrahiere Query-Parameter aus der OpenAPI-Spezifikation
          const queryParams = this.extractQueryParametersFromOperation(operation);

          endpoints.push({
            endpoint,
            pathParams,
            queryParams
          });
        }
      });
    });

    return endpoints;
  }

// ...existing code...
  /**
   * Extract path parameter names from an endpoint path.
   * Example: "/users/{userId}/posts/{postId}" -> ["userId", "postId"]
   */
  private extractPathParameters(endpointPath: string): string[] {
    const pathParamRegex = /\{([^}]+)\}/g;
    const matches: string[] = [];
    let match;
    
    while ((match = pathParamRegex.exec(endpointPath)) !== null) {
      matches.push(match[1]); // match[1] enthält den Namen ohne die Klammern
    }
    
    return matches;
  }

  /**
   * Extract query parameters from OpenAPI operation definition
   * Filters out non-parameter metadata fields
   */
  private extractQueryParametersFromOperation(operation: any): string[] {
    const queryParams: string[] = [];
    
    // Nur echte Parameter aus dem parameters-Array extrahieren
    if (operation.parameters && Array.isArray(operation.parameters)) {
      operation.parameters.forEach((param: any) => {
        // Prüfe, ob es sich um einen echten Query-Parameter handelt
        if (param.in === 'query' && param.name && this.isValidParameterName(param.name)) {
          queryParams.push(param.name);
        }
      });
    }
    
    return queryParams;
  }

  /**
   * Check if a parameter name is valid and not metadata
   */
  private isValidParameterName(paramName: string): boolean {
    // Liste von Metadaten-Feldern, die ignoriert werden sollen
    const metadataFields = [
      'additionalMetaData',
      'metadata',
      'meta',
      'additionalProperties',
      'extensions',
      'x-',  // OpenAPI extension fields start with x-
      '$',   // JSON Schema fields start with $
          // Internal fields often start with _
    ];

    // Prüfe, ob der Parameter-Name ein Metadaten-Feld ist
    for (const metaField of metadataFields) {
      if (paramName.toLowerCase().includes(metaField.toLowerCase()) || 
          paramName.startsWith(metaField)) {
        console.log(`🚫 Ignoring metadata field: ${paramName}`);
        return false;
      }
    }

    // Zusätzliche Prüfung: Parameter-Name sollte alphanumerisch sein
    if (!/^[a-zA-Z][a-zA-Z0-9_-]*$/.test(paramName)) {
      console.log(`🚫 Ignoring invalid parameter name: ${paramName}`);
      return false;
    }

    return true;
  }

  /**
// ...existing code...

  /**
// ...existing code...

  /**
   * Save discovered endpoints to the backend
   */
  private saveDiscoveredEndpoints(discoveredEndpoints: Array<{endpoint: CreateSourceSystemEndpointDTO, pathParams: string[], queryParams: string[]}>) {
    if (discoveredEndpoints.length === 0) {
      console.warn('No endpoints discovered');
      this.importing = false;
      return;
    }

    // Erstelle nur die Endpunkt-DTOs für den Batch-POST
    const endpointDTOs = discoveredEndpoints.map(item => item.endpoint);
    
    // Speichere alle Endpunkte auf einmal
    this.endpointSvc
      .apiConfigSourceSystemSourceSystemIdEndpointPost(this.sourceSystemId, endpointDTOs)
      .subscribe({
        next: () => {
          console.log(`Successfully created ${endpointDTOs.length} endpoints`);
          
          // Lade die Endpunkte neu, um die IDs zu bekommen
          this.loadEndpoints();
          
          // Warte kurz und erstelle dann die Parameter
          setTimeout(() => {
            this.createParametersForDiscoveredEndpoints(discoveredEndpoints);
          }, 1000);
        },
        error: (err) => {
          console.error('Failed to create endpoints:', err);
          this.importing = false;
        }
      });
  }

  /**
   * Create parameters for the discovered endpoints
   */
  private createParametersForDiscoveredEndpoints(discoveredEndpoints: Array<{endpoint: CreateSourceSystemEndpointDTO, pathParams: string[], queryParams: string[]}>) {
    // Lade die aktuellen Endpunkte, um die IDs zu bekommen
    this.endpointSvc.apiConfigSourceSystemSourceSystemIdEndpointGet(this.sourceSystemId)
      .subscribe({
        next: (currentEndpoints) => {
          discoveredEndpoints.forEach(discoveredItem => {
            // Finde den entsprechenden Endpunkt in der aktuellen Liste
            const matchingEndpoint = currentEndpoints.find(ep => 
              ep.endpointPath === discoveredItem.endpoint.endpointPath && 
              ep.httpRequestType === discoveredItem.endpoint.httpRequestType
            );

            if (matchingEndpoint && matchingEndpoint.id) {
              // Erstelle Pfad-Parameter
              discoveredItem.pathParams.forEach(paramName => {
                this.createQueryParam(matchingEndpoint.id!, paramName, ApiEndpointQueryParamType.Path);
              });

              // Erstelle Query-Parameter
              discoveredItem.queryParams.forEach(paramName => {
                this.createQueryParam(matchingEndpoint.id!, paramName, ApiEndpointQueryParamType.Query);
              });
            }
          });

          this.importing = false;
          console.log('Import completed successfully');
        },
        error: (err) => {
          console.error('Failed to load endpoints for parameter creation:', err);
          this.importing = false;
        }
      });
  }

  /**
   * Create a single query parameter for a specific endpoint.
   */
  private createQueryParam(endpointId: number, paramName: string, paramType: ApiEndpointQueryParamType) {
    const dto: ApiEndpointQueryParamDTO = {
      paramName: paramName,
      queryParamType: paramType
    };

    this.queryParamSvc
      .apiConfigEndpointEndpointIdQueryParamPost(endpointId, dto)
      .subscribe({
        next: () => {
          console.log(`Created ${paramType} parameter: ${paramName} for endpoint ${endpointId}`);
        },
        error: (err) => {
          console.error(`Failed to create parameter ${paramName}:`, err);
        }
      });
  }

  /**
// ...existing code...

  /**
   * Select an endpoint for detail management.
   * @param endpoint Endpoint to manage.
   */
  manage(endpoint: SourceSystemEndpointDTO) {
    console.log('Managing endpoint', endpoint);
    this.selectedEndpoint = endpoint;
    this.loadQueryParams(endpoint.id!); // Query-Parameter für den ausgewählten Endpunkt laden
  }

  /**
   * Load query parameters for a given endpoint.
   * @param endpointId The ID of the endpoint.
   */
  loadQueryParams(endpointId: number) {
    this.queryParamsLoading = true;
    this.queryParamSvc.apiConfigEndpointEndpointIdQueryParamGet(endpointId)
      .subscribe({
        next: (params) => {
          this.queryParams = params;
          this.queryParamsLoading = false;
        },
        error: (err) => {
          console.error('Failed to load query params', err);
          this.queryParamsLoading = false;
        }
      });
  }

  /**
   * Add a new query parameter to the selected endpoint.
   */
  addQueryParam() {
    if (this.queryParamForm.invalid || !this.selectedEndpoint?.id) {
      return;
    }
    const dto: ApiEndpointQueryParamDTO = this.queryParamForm.value;
    this.queryParamSvc.apiConfigEndpointEndpointIdQueryParamPost(this.selectedEndpoint.id, dto)
      .subscribe({
        next: () => {
          this.queryParamForm.reset({queryParamType: ApiEndpointQueryParamType.Query});
          this.loadQueryParams(this.selectedEndpoint!.id!);
        },
        error: (err) => console.error('Failed to add query param', err)
      });
  }

  /**
   * Delete a query parameter by its ID.
   * @param paramId The ID of the query parameter to delete.
   */
  deleteQueryParam(paramId: number) {
    this.queryParamSvc.apiConfigEndpointQueryParamIdDelete(paramId)
      .subscribe({
        next: () => {
          this.queryParams = this.queryParams.filter(p => p.id !== paramId);
        },
        error: (err) => console.error('Failed to delete query param', err)
      });
  }


  /**
   * Validator to ensure path parameter format: starts with { and ends with } with at least one character inside.
   */
  private pathParamFormatValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const parent = control.parent;
      // wenn Query-Typ, nie Fehler
      if (parent?.get('queryParamType')?.value === ApiEndpointQueryParamType.Query) {
        return null;
      }
  
      const val = control.value as string;
      // nur dann prüfen, wenn wirklich Path
      if (typeof val === 'string' && val.match(/^\{[A-Za-z0-9_]+\}$/)) {
        return null;
      }
      return { invalidPathParamFormat: true };
    };
  }

}
