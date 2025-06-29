import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { StepsModule } from 'primeng/steps';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextarea } from 'primeng/inputtextarea'; 
import { RadioButtonModule } from 'primeng/radiobutton';
import { InputGroupModule } from 'primeng/inputgroup';
import { TableModule } from 'primeng/table';
import {HttpClientModule } from '@angular/common/http';


import { SourceSystemApiService } from '../../../../services/source-system-api.service';
import { DiscoveredEndpointDto } from '../../../../models/discovered-endpoint.model';

interface Step {
  label: string;
}

@Component({
  selector: 'app-create-source-system',
  templateUrl: './create-source-system.component.html',
  styleUrls: ['./create-source-system.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DialogModule,
    StepsModule,
    DropdownModule,
    ButtonModule,
    InputTextModule,
    InputTextModule, 
    RadioButtonModule,
    InputGroupModule,
    TableModule,
  
    HttpClientModule,
  ]
})
export class CreateSourceSystemComponent implements OnInit {
  /** Whether the create dialog is currently visible */
  visible = false;

  /** Labels and order for each step in the wizard */
  steps: Step[] = [
    { label: 'Metadata' },
    { label: 'Endpoints' },
    { label: 'Specification' }
  ];

  /** Options for selecting the source system type */
  typeOptions = [
    { label: 'AAS', value: 'AAS' },
    { label: 'REST-OpenAPI', value: 'REST_OPENAPI' }
  ];
  /** Options for selecting the authentication type */
  authTypeOptions = [
    { label: 'Basic', value: 'BASIC' },
    { label: 'API Key', value: 'API_KEY' }
  ];

  /** FormGroup for the metadata step */
  form!: FormGroup;
  /** FormGroup for the endpoint creation step */
  step2Form!: FormGroup;

  /** Tracks the current step number in the wizard (1-based) */
  currentStep = 1;
  /** ID of the created source system, once saved in backend */
  createdSourceId: number | null = null;

  /** Endpoints discovered automatically from the OpenAPI spec */
  discoveredEndpoints: DiscoveredEndpointDto[] = [];
  /** HTTP methods available for manual endpoint creation */
  methodOptions = ['GET','POST','PUT','DELETE'];

  /** Manually added or edited endpoints before saving */
  manualEndpoints: {
    path: string;
    method: string;
    pollingRate?: number;
    schemaMode?: 'AUTO' | 'MANUAL';
    schema?: string;
  }[] = [];

  /** Controls visibility of the endpoint add/edit dialog */
  showEndpointDialog = false;
  /** Index of the endpoint currently being edited, or null for a new one */
  editingEndpointIndex: number | null = null;
  /** FormGroup for the add/edit endpoint dialog */
  endpointForm!: FormGroup;
  /** Available modes for schema generation */
  schemaModes = ['AUTO', 'MANUAL'];

  /** Currently selected specification file, if any */
  selectedFile: File | null = null;
  /** True if a file has been chosen in the form */
  fileSelected = false;

  constructor(
    private fb: FormBuilder,
    private api: SourceSystemApiService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      name:           ['', Validators.required],
      description:    [''],
      type:           ['REST_OPENAPI', Validators.required],
      apiUrl:         ['', Validators.required],
      authType:       ['NONE', Validators.required],
      username:       [''],
      password:       [''],
      apiKey:         [''],
      openApiSpecUrl: ['', [Validators.required, Validators.pattern('https?://.+')]],
    });

    this.step2Form = this.fb.group({
      path:   ['', Validators.required],
      method: ['', Validators.required]
    });

    // Form for Add/Edit Endpoint dialog
    this.endpointForm = this.fb.group({
      path: ['', Validators.required],
      method: ['', Validators.required],
      pollingRate: [1000, [Validators.required, Validators.min(0)]],
      schemaMode: ['AUTO', Validators.required],
      manualSchema: ['']
    });

    // Require credentials only when needed
    this.form.get('authType')!.valueChanges.subscribe(type => {
      const u = this.form.get('username')!, p = this.form.get('password')!, k = this.form.get('apiKey')!;
      u.clearValidators(); p.clearValidators(); k.clearValidators();
      if (type === 'BASIC') {
        u.setValidators([Validators.required]);
        p.setValidators([Validators.required]);
      } else if (type === 'API_KEY') {
        k.setValidators([Validators.required]);
      }
      u.updateValueAndValidity();
      p.updateValueAndValidity();
      k.updateValueAndValidity();
    });
  }

  /**
   * Opens the create source system dialog.
   * @returns void
   */
  open(): void {
    this.visible = true;
  }

  /**
   * Cancels and resets the dialog and form state.
   * @returns void
   */
  cancel(): void {
    this.visible = false;
    this.currentStep = 1;
    this.form.reset({
      type: 'REST_OPENAPI',
      authType: 'NONE'
    });
    this.step2Form.reset();
    this.selectedFile = null;
    this.fileSelected = false;
    this.discoveredEndpoints = [];
    this.createdSourceId = null;
  }

  /**
   * Handles file selection from the file input.
   * @param evt The file input change event
   * @returns void
   */
  onFileSelected(evt: Event): void {
    const inp = evt.target as HTMLInputElement;
    if (inp.files && inp.files.length) {
      this.selectedFile = inp.files[0];
      this.fileSelected = true;
      this.form.patchValue({ openApiSpecUrl: '' });
    }
  }

  /**
   * Proceeds to the next step in the wizard, saving data as needed.
   * @returns void
   */
  next(): void {
    if (this.currentStep === 1) {
      // CREATE SourceSystem
      if (this.fileSelected && this.selectedFile) {
        const fd = new FormData();
        fd.append('file', this.selectedFile);
        ['name','description','type','apiUrl','authType','username','password','apiKey']
          .forEach(k => fd.append(k, (this.form.value as any)[k] || ''));
        this.api.createFormData(fd).subscribe(res => {
          this.createdSourceId = res.id ?? null;
          this.loadEndpoints();
          this.discoverEndpoints();
          this.currentStep = 2;
        });
      } else {
        const dto = { ...this.form.value, openApiSpec: null };
        this.api.create(dto).subscribe(res => {
          this.createdSourceId = res.id ?? null;
          this.loadEndpoints();
          this.discoverEndpoints();
          this.currentStep = 2;
        });
      }
      return;
    }

    if (this.currentStep === 2) {
      this.currentStep = 3;
      return;
    }

    // final step
    this.cancel();
  }

  /**
   * Loads the list of endpoints for the created source system.
   * @returns void
   */
  private loadEndpoints(): void {
    if (!this.createdSourceId) return;
    this.api.listEndpoints(this.createdSourceId)
      .subscribe();
  }

  /**
   * Discovers endpoints automatically from the OpenAPI specification.
   * @returns void
   */
  discoverEndpoints(): void {
    console.log('▶ Discovering endpoints for sourceId=', this.createdSourceId);
    if (!this.createdSourceId) { return; }
    this.api.discoverEndpoints(this.createdSourceId).subscribe({
      next: list => {
        console.log('◀ Discovered:', list);
        this.discoveredEndpoints = list;
      },
      error: err => {
        console.error('✖ Discover error:', err);
      }
    });
  }

  /**
   * Adds an endpoint discovered automatically to the backend.
   * @param endpoint The discovered endpoint to add
   * @returns void
   */
  addDiscoveredEndpoint(endpoint: DiscoveredEndpointDto): void {
    if (!this.createdSourceId) {
      return;
    }
    this.api
      .createEndpoint(this.createdSourceId, {
        path: endpoint.path,
        method: endpoint.method
      })
      .subscribe(() => this.loadEndpoints());
  }

  /**
   * Adds a manually specified endpoint to the backend.
   * @returns void
   */
  addManualEndpoint(): void {
    if (!this.createdSourceId) return;
    if (this.step2Form.invalid) {
      this.step2Form.markAllAsTouched();
      return;
    }
    const { path, method } = this.step2Form.value;
    this.api.createEndpoint(this.createdSourceId, { path, method })
      .subscribe(() => {
        this.step2Form.reset();
        this.loadEndpoints();
      });
  }

  /**
   * Opens the Add/Edit Endpoint dialog.
   * @param index index of endpoint to edit, or null to add new.
   * @returns void
   */
  openEndpointDialog(index: number | null = null): void {
    this.editingEndpointIndex = index;
    if (index !== null && this.manualEndpoints[index]) {
      const ep = this.manualEndpoints[index];
      this.endpointForm.setValue({
        path: ep.path,
        method: ep.method,
        pollingRate: ep.pollingRate ?? 1000,
        schemaMode: ep.schemaMode ?? 'AUTO',
        manualSchema: ep.schema ?? ''
      });
    } else {
      this.endpointForm.reset({ pollingRate: 1000, schemaMode: 'AUTO', path: '', method: '', manualSchema: '' });
    }
    this.showEndpointDialog = true;
  }

  /**
   * Saves the endpoint from the dialog into the manual endpoints list.
   * @returns void
   */
  saveEndpoint(): void {
    if (this.endpointForm.invalid) {
      this.endpointForm.markAllAsTouched();
      return;
    }
    const val = this.endpointForm.value;
    const newEp = {
      path: val.path,
      method: val.method,
      pollingRate: val.pollingRate,
      schemaMode: val.schemaMode,
      schema: val.schemaMode === 'MANUAL' ? val.manualSchema : undefined
    };
    if (this.editingEndpointIndex !== null) {
      this.manualEndpoints[this.editingEndpointIndex] = newEp;
    } else {
      this.manualEndpoints.push(newEp);
    }
    this.showEndpointDialog = false;
  }
}