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
import { FileUploadModule } from 'primeng/fileupload';

import { CreateSourceSystemDTO } from '../../../../models/create-source-system.dto';
import { SourceSystemEndpointDTO } from '../../../../models/source-system-endpoint.dto';
import { CreateSourceSystemEndpointDTO } from '../../../../models/create-source-system-endpoint.dto';
import { ApiHeaderDto } from '../../../../models/api-header.dto';
import { CreateApiHeaderDto } from '../../../../models/create-api-header.dto';
import { ApiEndpointQueryParamDTO } from '../../../../models/api-endpoint-query-param.dto';
import { CreateRequestConfigurationDto } from '../../../../models/create-request-configuration.dto';
import { GetRequestConfigurationDto } from '../../../../models/get-request-configuration.dto';

interface Step {
  label: string;
}

interface ManualEndpoint {
  path: string;
  method: string;
}

interface DiscoveredEndpointDto {
  path: string;
  method: string;
}

@Component({
  standalone: true,
  selector: 'app-create-source-system',
  templateUrl: './create-source-system.component.html',
  styleUrls: ['./create-source-system.component.css'],
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
    FileUploadModule,
  ]
})
export class CreateSourceSystemComponent implements OnInit {
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();
  @Output() sourceSaved = new EventEmitter<void>();

  steps: Step[] = [
    { label: 'Metadata' },
    { label: 'Endpoints' },
    { label: 'Headers' },
    { label: 'Query Params' },
    { label: 'Request Config' },
    { label: 'Specification' },
  ];
  currentStep = 1;

  typeOptions = [
    { label: 'AAS', value: 'AAS' },
    { label: 'REST-OpenAPI', value: 'REST_OPENAPI' }
  ];
  authTypeOptions = [
    { label: 'None', value: 'NONE' },
    { label: 'Basic', value: 'BASIC' },
    { label: 'API Key', value: 'API_KEY' }
  ];

  form!: FormGroup;
  endpointForm!: FormGroup;
  headerForm!: FormGroup;
  queryParamForm!: FormGroup;
  requestConfigForm!: FormGroup;

  discoveredEndpoints: DiscoveredEndpointDto[] = [];
  manualEndpoints: ManualEndpoint[] = [];

  manualHeaders: ApiHeaderDto[] = [];
  manualQueryParams: ApiEndpointQueryParamDTO[] = [];
  manualRequestConfigs: CreateRequestConfigurationDto[] = [];

  selectedFile: File | null = null;
  fileSelected = false;

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    this.initializeForms();
    this.setupAuthTypeValidators();
  }

  private initializeForms(): void {
    // Step 1 form
    this.form = this.fb.group({
      name: ['', Validators.required],
      description: [''],
      type: ['REST_OPENAPI', Validators.required],
      apiUrl: ['', [Validators.required, Validators.pattern('https?://.+')]],
      authType: ['NONE', Validators.required],
      username: [''],
      password: [''],
      apiKey: [''],
      openApiSpecUrl: ['', Validators.pattern('https?://.+')],
    });

    // Step 2 dialog form
    this.endpointForm = this.fb.group({
      path: ['', Validators.required],
      method: ['', Validators.required],
    });

    // Step 3 dialog form
    this.headerForm = this.fb.group({
      headerType: ['API_KEY', Validators.required],
      headerName: ['', Validators.required],
      values: ['', Validators.required], // comma-separated
    });

    // Step 4 dialog form
    this.queryParamForm = this.fb.group({
      paramName: ['', Validators.required],
      queryParamType: ['QUERY', Validators.required],
      values: ['', Validators.required],
    });

    // Step 5 dialog form
    this.requestConfigForm = this.fb.group({
      name: ['', Validators.required],
      active: [true],
      pollingIntervalTimeInMs: [1000, [Validators.required, Validators.min(0)]],
    });
  }

  private setupAuthTypeValidators(): void {
    this.form.get('authType')!.valueChanges.subscribe(type => {
      const usernameControl = this.form.get('username')!;
      const passwordControl = this.form.get('password')!;
      const apiKeyControl = this.form.get('apiKey')!;

      usernameControl.clearValidators();
      passwordControl.clearValidators();
      apiKeyControl.clearValidators();

      if (type === 'BASIC') {
        usernameControl.setValidators([Validators.required]);
        passwordControl.setValidators([Validators.required]);
      } else if (type === 'API_KEY') {
        apiKeyControl.setValidators([Validators.required]);
      }

      usernameControl.updateValueAndValidity();
      passwordControl.updateValueAndValidity();
      apiKeyControl.updateValueAndValidity();
    });
  }

  open(): void {
    this.visible = true;
  }

  cancel(): void {
    this.visible = false;
    this.currentStep = 1;
    this.form.reset({ type: 'REST_OPENAPI', authType: 'NONE' });
    this.selectedFile = null;
    this.fileSelected = false;
    this.discoveredEndpoints = [];
    this.manualEndpoints = [];
    this.manualHeaders = [];
    this.manualQueryParams = [];
    this.manualRequestConfigs = [];
  }

  next(): void {
    if (this.currentStep < this.steps.length) {
      this.currentStep++;
      if (this.currentStep === 2) {
        this.onCreateSourceSystem();
      }
    } else {
      this.cancel();
    }
  }

  private onCreateSourceSystem(): void {
    const fakeId = Math.floor(Math.random() * 10000);
    this.discoverEndpoints();
  }

  discoverEndpoints(): void {
    if (this.selectedFile) {
      const reader = new FileReader();
      reader.onload = () => {
        try {
          const spec = JSON.parse(reader.result as string);
          this.discoveredEndpoints = Object.entries(spec.paths || {}).flatMap(
            ([path, methods]: any) =>
              Object.keys(methods).map(m => ({ path, method: m.toUpperCase() }))
          );
        } catch (e) {
          console.error('Spec parse error', e);
        }
      };
      reader.readAsText(this.selectedFile);
    } else if (this.form.value.openApiSpecUrl) {
      fetch(this.form.value.openApiSpecUrl)
        .then(res => res.json())
        .then(spec => {
          this.discoveredEndpoints = Object.entries(spec.paths || {}).flatMap(
            ([path, methods]: any) =>
              Object.keys(methods).map(m => ({ path, method: m.toUpperCase() }))
          );
        })
        .catch(err => console.error('Fetch spec error', err));
    }
  }

  openEndpointDialog(index: number | null = null): void {
    if (index !== null) {
      const ep = this.manualEndpoints[index];
      this.endpointForm.setValue({ path: ep.path, method: ep.method });
    } else {
      this.endpointForm.reset();
    }
    this.visible = true;
  }

  saveEndpoint(): void {
    const { path, method } = this.endpointForm.value;
    this.manualEndpoints.push({ path, method });
    this.visible = false;
  }

  openHeaderDialog(index: number | null = null): void {
    if (index !== null) {
      const h = this.manualHeaders[index];
      this.headerForm.setValue({
        headerType: h.headerType,
        headerName: h.headerName,
        values: h.values.join(','),
      });
    } else {
      this.headerForm.reset();
    }
    this.visible = true;
  }

  saveHeader(): void {
    const { headerType, headerName, values } = this.headerForm.value;
    const vals = values.split(',').map((s: string) => s.trim());
    this.manualHeaders.push({ id: 0, headerType, headerName, values: vals });
    this.visible = false;
  }

  openQueryParamDialog(index: number | null = null): void {
    if (index !== null) {
      const q = this.manualQueryParams[index];
      this.queryParamForm.setValue({
        paramName: q.paramName,
        queryParamType: q.queryParamType,
        values: q.values.join(','),
      });
    } else {
      this.queryParamForm.reset();
    }
    this.visible = true;
  }

  saveQueryParam(): void {
    const { paramName, queryParamType, values } = this.queryParamForm.value;
    const vals = values.split(',').map((s: string) => s.trim());
    this.manualQueryParams.push({ paramName, queryParamType, id: 0, values: vals });
    this.visible = false;
  }

  openRequestConfigDialog(index: number | null = null): void {
    if (index !== null) {
      const c = this.manualRequestConfigs[index];
      this.requestConfigForm.setValue({
        name: c.name,
        active: c.active,
        pollingIntervalTimeInMs: c.pollingIntervalTimeInMs,
      });
    } else {
      this.requestConfigForm.reset();
    }
    this.visible = true;
  }

  saveRequestConfig(): void {
    const cfg = this.requestConfigForm.value as CreateRequestConfigurationDto;
    this.manualRequestConfigs.push(cfg);
    this.visible = false;
  }

  onFileSelected(evt: any): void {
    const file = evt.files ? evt.files[0] : evt.target.files[0];
    if (file) {
      this.selectedFile = file;
      this.fileSelected = true;
      this.form.patchValue({ openApiSpecUrl: '' });
    }
  }
}