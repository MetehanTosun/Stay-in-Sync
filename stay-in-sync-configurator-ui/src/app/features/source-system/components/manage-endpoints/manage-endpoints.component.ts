// src/app/features/source-system/components/manage-endpoints/manage-endpoints.component.ts
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

import { SourceSystemEndpointResourceService } from '../../../../generated/api/sourceSystemEndpointResource.service';
import { HttpClient } from '@angular/common/http';
import { SourceSystemResourceService } from '../../../../generated/api/sourceSystemResource.service';

import { SourceSystemEndpointDTO }       from '../../../../generated';
import { CreateSourceSystemEndpointDTO } from '../../../../generated';

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
  @Input() sourceSystemId!: number;
  @Output() backStep = new EventEmitter<void>();
  @Output() finish   = new EventEmitter<void>();

  // Endpoints
  endpoints: SourceSystemEndpointDTO[] = [];
  endpointForm!: FormGroup;
  loading = false;

  // Selected Endpoint
  selectedEndpoint: SourceSystemEndpointDTO | null = null;

  // HTTP methods dropdown
  httpRequestTypes = [
    { label: 'GET',    value: 'GET'    },
    { label: 'POST',   value: 'POST'   },
    { label: 'PUT',    value: 'PUT'    },
    { label: 'DELETE', value: 'DELETE' }
  ];

  // Importing endpoints from the external API
  apiUrl: string | null = null;
  importing = false;

  constructor(
    private fb: FormBuilder,
    private endpointSvc: SourceSystemEndpointResourceService,
    private sourceSystemService: SourceSystemResourceService,
    private http: HttpClient,
  ) {}

  ngOnInit(): void {
    // Endpoint form
    this.endpointForm = this.fb.group({
      endpointPath:    ['', Validators.required],
      httpRequestType: ['GET', Validators.required]
    });

    this.loadEndpoints();
    // Load the Source System base URL
    this.sourceSystemService
      .apiConfigSourceSystemIdGet(this.sourceSystemId, 'body')
      .subscribe(ss => this.apiUrl = ss.apiUrl);

    // setup edit form
    this.editForm = this.fb.group({
      endpointPath:    ['', Validators.required],
      httpRequestType: ['GET', Validators.required]
    });
  }

  // --- Endpoints CRUD ---
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

  deleteEndpoint(id: number) {
    this.endpointSvc
      .apiConfigSourceSystemEndpointIdDelete(id)
      .subscribe({
        next: () => this.endpoints = this.endpoints.filter(e => e.id !== id),
        error: console.error
      });
  }

  // Switch to Request-Config pane
  manage(endpoint: SourceSystemEndpointDTO) {
    console.log('Managing endpoint', endpoint);
    this.selectedEndpoint = endpoint;
  }

  /** Opens the edit dialog for a given endpoint */
  // does not work
  openEditDialog(endpoint: SourceSystemEndpointDTO) {
    console.log('Opening edit dialog for endpoint', endpoint);
    this.editingEndpoint = endpoint;
    this.editForm.patchValue({
      endpointPath: endpoint.endpointPath,
      httpRequestType: endpoint.httpRequestType
    });
    this.editDialog = true;
  }

  /** Saves the edited endpoint via PUT */
  // Does not work
  saveEdit() {
    if (!this.editingEndpoint || this.editForm.invalid) {
      return;
    }
    console.log('saveEdit called, editingEndpoint:', this.editingEndpoint);
    // Construct full SourceSystemEndpointDTO with required fields
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

  // Navigation handlers
  onBack()   { this.backStep.emit();   }
  onFinish(){ this.finish.emit();     }

  // Editing endpoints
  editDialog: boolean = false;
  editingEndpoint: SourceSystemEndpointDTO | null = null;
  editForm!: FormGroup;

  /**
   * Imports endpoints from the configured source system's OpenAPI spec
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
}