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
import { SourceSystemDTO } from '../../models/sourceSystemDTO';


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

    this.loadSourceSystemAndSetApiUrl();
   
  }

  private loadSourceSystemAndSetApiUrl(): void {
    this.sourceSystemService.apiConfigSourceSystemIdGet(this.sourceSystemId)
      .subscribe({
        next: (sourceSystem: SourceSystemDTO) => {
          console.log('Loaded source system:', sourceSystem);
          console.log('openApiSpec field:', sourceSystem.openApiSpec);
          console.log('openApiSpec type:', typeof sourceSystem.openApiSpec);
          
          // Setze die API-URL für den Import
          if (sourceSystem.openApiSpec && typeof sourceSystem.openApiSpec === 'string') {
            // Prüfe, ob openApiSpec eine URL ist (beginnt mit http)
            if (sourceSystem.openApiSpec.startsWith('http')) {
              this.apiUrl = sourceSystem.openApiSpec.trim();
              console.log('✅ Found OpenAPI URL in source system:', this.apiUrl);
            } else {
              // Falls es Datei-Inhalt ist, aber wir brauchen eine URL
              console.log('ℹ️ OpenAPI spec contains file content, not URL. Using API URL instead.');
              this.apiUrl = sourceSystem.apiUrl!;
            }
          } else {
            // Fallback: Verwende die API-URL
            this.apiUrl = sourceSystem.apiUrl!;
            console.log('ℹ️ No OpenAPI spec found, using API URL:', this.apiUrl);
          }
        },
        error: (err: any) => {
          console.error('Failed to load source system:', err);
          // Fallback: Verwende eine Standard-URL (falls verfügbar)
          this.apiUrl = 'https://petstore.swagger.io/v2';
        }
      });
  }
// ...existing code...
// ...existing code...
// ...existing code...
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

  importEndpoints() {
    if (!this.apiUrl) {
      console.error('No API URL available for import');
      return;
    }
    
    this.importing = true;
    
    // Wenn apiUrl bereits eine direkte OpenAPI-URL ist, verwende sie direkt
    if (this.apiUrl.includes('swagger.json') || this.apiUrl.includes('openapi.json')) {
      console.log('Using direct OpenAPI URL:', this.apiUrl);
      this.http.get(this.apiUrl).subscribe({
        next: (openApiSpec: any) => {
          console.log('✅ Loaded OpenAPI spec from direct URL');
          this.processOpenApiSpec(openApiSpec);
        },
        error: (err) => {
          console.error('Failed to load OpenAPI spec from direct URL:', err);
          this.importing = false;
        }
      });
    } else {
      // Versuche Standard-Pfade an die API-URL anzuhängen
      this.http.get(this.apiUrl + '/swagger.json').subscribe({
        next: (openApiSpec: any) => {
          console.log('✅ Loaded OpenAPI spec from /swagger.json');
          this.processOpenApiSpec(openApiSpec);
        },
        error: (err) => {
          console.log('ℹ️ /swagger.json not found, trying alternative URLs...');
          this.tryAlternativeOpenApiUrls();
        }
      });
    }
  }
// ...existing code...


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
    'https://raw.githubusercontent.com/open-meteo/open-meteo/main/openapi.yml'
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


  /**
   * Extract path parameter names from an endpoint path.
   * Example: "/users/{userId}/posts/{postId}" -> ["userId", "postId"]
   */
  private extractPathParameters(endpointPath: string): string[] {
    const pathParamRegex = /\{([^}]+)\}/g;
    const matches: string[] = [];
    let match;
    
    while ((match = pathParamRegex.exec(endpointPath)) !== null) {
      matches.push(match[1]); 
    }
    
    return matches;
  }

  /**
   * Extract query parameters from OpenAPI operation definition
   * Filters out non-parameter metadata fields
   */
  private extractQueryParametersFromOperation(operation: any): string[] {
    const queryParams: string[] = [];
    
    
    if (operation.parameters && Array.isArray(operation.parameters)) {
      operation.parameters.forEach((param: any) => {
       
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
    
    const metadataFields = [
      'additionalMetaData',
      'metadata',
      'meta',
      'additionalProperties',
      'extensions',
      'x-',  
      '$',    
    ];

    
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
   * Save discovered endpoints to the backend
   */
  private saveDiscoveredEndpoints(discoveredEndpoints: Array<{endpoint: CreateSourceSystemEndpointDTO, pathParams: string[], queryParams: string[]}>) {
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
    
    this.endpointSvc.apiConfigSourceSystemSourceSystemIdEndpointGet(this.sourceSystemId)
      .subscribe({
        next: (currentEndpoints) => {
          discoveredEndpoints.forEach(discoveredItem => {
            
            const matchingEndpoint = currentEndpoints.find(ep => 
              ep.endpointPath === discoveredItem.endpoint.endpointPath && 
              ep.httpRequestType === discoveredItem.endpoint.httpRequestType
            );

            if (matchingEndpoint && matchingEndpoint.id) {
              
              discoveredItem.pathParams.forEach(paramName => {
                this.createQueryParam(matchingEndpoint.id!, paramName, ApiEndpointQueryParamType.Path);
              });

             
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
   * Select an endpoint for detail management.
   * @param endpoint Endpoint to manage.
   */
  manage(endpoint: SourceSystemEndpointDTO) {
    console.log('Managing endpoint', endpoint);
    this.selectedEndpoint = endpoint;
    this.loadQueryParams(endpoint.id!); 
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
      
      if (parent?.get('queryParamType')?.value === ApiEndpointQueryParamType.Query) {
        return null;
      }
  
      const val = control.value as string;
     
      if (typeof val === 'string' && val.match(/^\{[A-Za-z0-9_]+\}$/)) {
        return null;
      }
      return { invalidPathParamFormat: true };
    };
  }

}
