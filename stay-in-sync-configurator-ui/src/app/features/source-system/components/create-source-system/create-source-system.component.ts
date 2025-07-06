import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';

// PrimeNG
import { DialogModule } from 'primeng/dialog';
import { StepsModule } from 'primeng/steps';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { TextareaModule } from 'primeng/textarea';

// Services und DTOs
import { SourceSystemResourceService } from '../../../../generated/api/sourceSystemResource.service';
import { CreateSourceSystemDTO } from '../../../../generated/model/createSourceSystemDTO';
import { ApiAuthType } from '../../../../generated/model/apiAuthType';
import { BasicAuthDTO } from '../../../../generated/model/basicAuthDTO';
import { ApiKeyAuthDTO } from '../../../../generated/model/apiKeyAuthDTO';
import { ManageEndpointsComponent } from '../manage-endpoints/manage-endpoints.component';
import { HttpResponse } from '@angular/common/http';

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
    InputTextModule,
    ButtonModule,
    TextareaModule,
    ManageEndpointsComponent
  ]
})
export class CreateSourceSystemComponent implements OnInit {
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();

  steps = [
    { label: 'Create Source System' },
    { label: 'Manage Endpoints' }
  ];
  currentStep = 0; // Start bei Schritt 0 (Metadaten)
  createdSourceSystemId!: number;

  form!: FormGroup;
  selectedFile: File | null = null;
  fileSelected = false;

  typeOptions = [
    { label: 'REST-OpenAPI', value: 'REST_OPENAPI' },
    { label: 'AAS', value: 'AAS' }
  ];
  authTypeOptions = [
    { label: 'Basic', value: ApiAuthType.Basic },
    { label: 'API Key', value: ApiAuthType.ApiKey }
  ];

  constructor(
    private fb: FormBuilder,
    private sourceSystemService: SourceSystemResourceService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      name: ['', Validators.required],
      apiUrl: ['', [Validators.required, Validators.pattern('https?://.+')]],
      description: [''],
      apiType: ['REST_OPENAPI', Validators.required],
      apiAuthType: [null],
      authConfig: this.fb.group({
        username: [''],
        password: [''],
        apiKey: [''],
        headerName: ['']
      }),
      openApiSpec: [{ value: null, disabled: false }]
    });

    this.form.get('apiAuthType')!.valueChanges.subscribe((authType: ApiAuthType) => {
      const grp = this.form.get('authConfig') as FormGroup;
      // reset
      ['username', 'password', 'apiKey', 'headerName'].forEach(k => {
        grp.get(k)!.clearValidators();
        grp.get(k)!.updateValueAndValidity();
      });
      if (authType === ApiAuthType.Basic) {
        grp.get('username')!.setValidators([Validators.required]);
        grp.get('password')!.setValidators([Validators.required]);
      } else if (authType === ApiAuthType.ApiKey) {
        grp.get('apiKey')!.setValidators([Validators.required]);
        grp.get('headerName')!.setValidators([Validators.required]);
      }
      ['username', 'password', 'apiKey', 'headerName'].forEach(k => grp.get(k)!.updateValueAndValidity());
    });
  }

  onFileSelected(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    if (input.files?.length) {
      this.selectedFile = input.files[0];
      this.fileSelected = true;
      this.form.get('openApiSpec')!.disable();
    }
  }

  cancel(): void {
    this.visible = false;
    this.visibleChange.emit(false);
    // reset form and state
    this.form.reset({ apiType: 'REST_OPENAPI', apiAuthType: null });
    this.selectedFile = null;
    this.fileSelected = false;
    this.form.get('openApiSpec')!.enable();
    this.currentStep = 0;
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const base = { ...this.form.getRawValue() } as CreateSourceSystemDTO;
    delete base.authConfig;
    delete base.openApiSpec;

    const authType = this.form.get('apiAuthType')!.value as ApiAuthType;
    const cfg = this.form.get('authConfig')!.value;
    if (authType === ApiAuthType.Basic) {
      base.authConfig = { authType, username: cfg.username, password: cfg.password } as BasicAuthDTO;
    } else if (authType === ApiAuthType.ApiKey) {
      base.authConfig = { authType, apiKey: cfg.apiKey, headerName: cfg.headerName } as ApiKeyAuthDTO;
    }

    const post = () => {
      this.postDto(base);
    };

    if (this.selectedFile) {
      const reader = new FileReader();
      reader.onload = () => {
        const b64 = (reader.result as string).split(',')[1];
        const bytes = atob(b64);
        const arr = new Uint8Array(bytes.length);
        for (let i = 0; i < bytes.length; i++) arr[i] = bytes.charCodeAt(i);
        base.openApiSpec = new Blob([arr], { type: 'application/octet-stream' });
        post();
      };
      reader.readAsDataURL(this.selectedFile);
    } else {
      post();
    }
  }

  private postDto(dto: CreateSourceSystemDTO): void {
    this.sourceSystemService
      .apiConfigSourceSystemPost(dto, 'response', false)
      .subscribe({
        next: (resp: HttpResponse<void>) => {
          const location = resp.headers.get('Location');
          if (!location) {
            console.error('No Location header returned');
            return;
          }
          const parts = location.split('/');
          const id = Number(parts[parts.length - 1]);
          if (isNaN(id)) {
            console.error('Cannot parse ID from Location header:', location);
            return;
          }
          this.createdSourceSystemId = id;
          this.currentStep = 1; // Wechsel zu Schritt 1 (Manage Endpoints)
        },
        error: (err) => {
          console.error('Failed to create Source System:', err);
        }
      });
  }

  /** Proceeds to the next step: either create or advance */
  goNext(): void {
    // if on first step, create the source system
    if (this.currentStep === 0) {
      this.save();
    }
  }

  /** Returns to the previous step */
  goBack(): void {
    if (this.currentStep > 0) {
      this.currentStep = 0;
    }
  }
}