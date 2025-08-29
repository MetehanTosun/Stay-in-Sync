import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { TextareaModule } from 'primeng/textarea';
import { StepsModule } from 'primeng/steps';
import { FileUploadModule } from 'primeng/fileupload';

import { TargetSystemDTO } from '../../models/targetSystemDTO';
import { TargetSystemResourceService } from '../../service/targetSystemResource.service';
import { ManageTargetEndpointsComponent } from '../manage-target-endpoints/manage-target-endpoints.component';
import { ManageApiHeadersComponent } from '../../../source-system/components/manage-api-headers/manage-api-headers.component';
import { ApiHeaderResourceService } from '../../../source-system/service/apiHeaderResource.service';
import { ApiAuthType } from '../../../source-system/models/apiAuthType';
import { HttpErrorService } from '../../../../core/services/http-error.service';

@Component({
  standalone: true,
  selector: 'app-create-target-system',
  templateUrl: './create-target-system.component.html',
  styleUrls: [],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DialogModule,
    DropdownModule,
    InputTextModule,
    ButtonModule,
    TextareaModule,
    StepsModule,
    FileUploadModule,
    ManageTargetEndpointsComponent,
    ManageApiHeadersComponent
  ]
})
export class CreateTargetSystemComponent implements OnInit, OnChanges {
  @Input() visible = false;
  @Input() targetSystem: TargetSystemDTO | null = null;
  @Output() visibleChange = new EventEmitter<boolean>();

  steps = [
    { label: 'Metadata' },
    { label: 'API Headers' },
    { label: 'Endpoints' },
  ];
  currentStep = 0;
  createdTargetSystemId!: number;

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
  public readonly ApiAuthType = ApiAuthType;

  constructor(
    private fb: FormBuilder,
    private api: TargetSystemResourceService,
    private headersApi: ApiHeaderResourceService,
    protected errorService: HttpErrorService,
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

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['targetSystem'] && this.targetSystem) {
      this.form.patchValue({
        name: this.targetSystem.name,
        apiUrl: this.targetSystem.apiUrl,
        description: this.targetSystem.description,
        apiType: this.targetSystem.apiType
      });
      this.currentStep = 0;
    }
  }

  onFileSelected(event: any): void {
    this.selectedFile = event.files?.[0] ?? null;
    this.fileSelected = true;
    this.form.get('openApiSpec')!.disable();
  }

  cancel(): void {
    this.visible = false;
    this.visibleChange.emit(false);
    this.form.reset({ apiType: 'REST_OPENAPI' });
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

    const base: any = { ...this.form.getRawValue() };

    const post = (payload: TargetSystemDTO) => {
      this.api.create(payload).subscribe({
        next: (resp) => {
          this.createdTargetSystemId = resp.id!;
          this.currentStep = 1;
        },
        error: (err) => {
          this.errorService.handleError(err);
        }
      });
    };

    if (this.selectedFile) {
      const reader = new FileReader();
      reader.onload = () => {
        const fileContent = reader.result as string;
        (base as any).openAPI = fileContent;
        post(base);
      };
      reader.readAsText(this.selectedFile);
    } else {
      const val = (base as any).openApiSpec;
      if (typeof val === 'string' && val.trim()) {
        (base as any).openAPI = val;
      }
      delete (base as any).openApiSpec;
      // Strip auth fields not used by backend DTO
      delete base.apiAuthType;
      delete base.authConfig;
      post(base as TargetSystemDTO);
    }
  }

  goNext(): void {
    if (this.currentStep === 0) {
      this.save();
    } else if (this.currentStep === 1) {
      this.currentStep = 2;
    }
  }

  goBack(): void {
    if (this.currentStep > 0) this.currentStep -= 1;
  }
}


