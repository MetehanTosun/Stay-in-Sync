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

// Services und DTOs
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
  /** Controls dialog visibility */
  visible = false;

  /** Wizard steps */
  steps: Step[] = [
    { label: 'Metadata' },
    { label: 'Endpoints' },
    { label: 'Specification' }
  ];

  /** Dropdown options */
  typeOptions = [
    { label: 'AAS', value: 'AAS' },
    { label: 'REST-OpenAPI', value: 'REST_OPENAPI' }
  ];
  authTypeOptions = [
    { label: 'Basic', value: 'BASIC' },
    { label: 'API Key', value: 'API_KEY' }
  ];

  /** Forms */
  form!: FormGroup;
  step2Form!: FormGroup;

  /** Wizard state */
  currentStep = 1;
  createdSourceId: number | null = null;

  /** Step-2 state */
  discoveredEndpoints: DiscoveredEndpointDto[] = [];
  methodOptions = ['GET','POST','PUT','DELETE'];

  /** Manually added endpoints with optional polling and schema settings */
  manualEndpoints: {
    path: string;
    method: string;
    pollingRate?: number;
    schemaMode?: 'AUTO' | 'MANUAL';
    schema?: string;
  }[] = [];

  /** Dialog state for adding/editing endpoints */
  showEndpointDialog = false;
  editingEndpointIndex: number | null = null;
  endpointForm!: FormGroup;
  schemaModes = ['AUTO', 'MANUAL'];

  /** File upload */
  selectedFile: File | null = null;
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

  open(): void {
    this.visible = true;
  }

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

  onFileSelected(evt: Event): void {
    const inp = evt.target as HTMLInputElement;
    if (inp.files && inp.files.length) {
      this.selectedFile = inp.files[0];
      this.fileSelected = true;
      this.form.patchValue({ openApiSpecUrl: '' });
    }
  }

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

  private loadEndpoints(): void {
    if (!this.createdSourceId) return;
    this.api.listEndpoints(this.createdSourceId)
      .subscribe();
  }

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
   * Open the Add/Edit Endpoint dialog.
   * @param index index of endpoint to edit, or null to add new.
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
   * Save the endpoint from the dialog into the list.
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