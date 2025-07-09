import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';

import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogModule }   from 'primeng/dialog';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import { SourceSystemEndpointResourceService } from '../../generated/api/sourceSystemEndpointResource.service';
import { HttpClient } from '@angular/common/http';
import { SourceSystemResourceService } from '../../generated/api/sourceSystemResource.service';

import { SourceSystemEndpointDTO }       from '../../generated';
import { CreateSourceSystemEndpointDTO } from '../../generated';
import { ApiEndpointQueryParamDTO } from '../../generated/model/apiEndpointQueryParamDTO'; // Importieren
import { ApiEndpointQueryParamType } from '../../generated/model/apiEndpointQueryParamType'; // Importieren
import { ApiEndpointQueryParamResourceService } from '../../generated/api/apiEndpointQueryParamResource.service';


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
  @Output() finish   = new EventEmitter<void>();

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
    { label: 'GET',    value: 'GET'    },
    { label: 'POST',   value: 'POST'   },
    { label: 'PUT',    value: 'PUT'    },
    { label: 'DELETE', value: 'DELETE' }
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
      { label: 'Query', value: ApiEndpointQueryParamType.Query },
      { label: 'Path',  value: ApiEndpointQueryParamType.Path  }
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
  ) {}

  /**
   * Initialize forms and load endpoints and source system API URL.
   */
  ngOnInit(): void {
    this.endpointForm = this.fb.group({
      endpointPath:    ['', Validators.required],
      httpRequestType: ['GET', Validators.required]
    });

    this.queryParamForm = this.fb.group({
      paramName: ['', Validators.required],
      queryParamType: [ApiEndpointQueryParamType.Query, Validators.required]
    });

    this.loadEndpoints();
    this.sourceSystemService
      .apiConfigSourceSystemIdGet(this.sourceSystemId, 'body')
      .subscribe(ss => this.apiUrl = ss.apiUrl);

    this.editForm = this.fb.group({
      endpointPath:    ['', Validators.required],
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
          this.endpointForm.reset({ httpRequestType: 'GET' });
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
  onBack()   { this.backStep.emit();   }
  /**
   * Finish the wizard and emit completion event.
   */
  onFinish(){ this.finish.emit();     }

  /**
   * Attempt to import endpoints by fetching an OpenAPI spec and pushing to backend.
   */
  importEndpoints() {
    if (!this.apiUrl) return;
    this.importing = true;
    const tryLoad = (url: string) => this.http.get<any>(url).toPromise();
    const paths = [
      '/v2/swagger.json',
      '/v2/openapi.json',
      '/openapi.json',
      '/swagger.json',
      '/v3/api-docs',
      '/v3/api-docs.json',
      '/v3/openapi.json',
      '/v3/swagger.json',
      '/api/v3/openapi.json',
    ];
    (async () => {
      let spec: any = null;
      for (const p of paths) {
        try {
          spec = await tryLoad(`${this.apiUrl}${p}`);
          console.log('Loaded spec from', p);
          break;
        } catch (_) {}
      }
      if (!spec) {
        this.importing = false;
        console.error('Failed to load any OpenAPI spec');
        return;
      }
      const dtos: CreateSourceSystemEndpointDTO[] = [];
      for (const [path, methods] of Object.entries(spec.paths || {})) {
        for (const m of Object.keys(methods as object)) {
          dtos.push({ endpointPath: path, httpRequestType: m.toUpperCase() });
        }
      }
      this.endpointSvc
        .apiConfigSourceSystemSourceSystemIdEndpointPost(this.sourceSystemId, dtos)
        .subscribe({
          next: () => { this.importing = false; this.loadEndpoints(); },
          error: err => { console.error('Import failed', err); this.importing = false; }
        });
    })();
  }

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
        this.queryParamForm.reset({ queryParamType: ApiEndpointQueryParamType.Query });
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


}