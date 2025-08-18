import {Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges} from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {CommonModule} from '@angular/common';
import {SourceSystemDTO} from '../../models/sourceSystemDTO';


import {DialogModule} from 'primeng/dialog';
import {DropdownModule} from 'primeng/dropdown';
import {InputTextModule} from 'primeng/inputtext';
import {ButtonModule} from 'primeng/button';
import {TextareaModule} from 'primeng/textarea';
import {StepsModule} from 'primeng/steps';
import {FileSelectEvent, FileUploadEvent, FileUploadModule} from 'primeng/fileupload';



import {SourceSystemResourceService} from '../../service/sourceSystemResource.service';
import {CreateSourceSystemDTO} from '../../models/createSourceSystemDTO';
import {ApiAuthType} from '../../models/apiAuthType';
import {BasicAuthDTO} from '../../models/basicAuthDTO';
import {ApiKeyAuthDTO} from '../../models/apiKeyAuthDTO';
import {ManageEndpointsComponent} from '../manage-endpoints/manage-endpoints.component';
import {ManageApiHeadersComponent} from '../manage-api-headers/manage-api-headers.component';
import {HttpErrorService} from '../../../../core/services/http-error.service';

/**
 * Component for creating or editing a Source System.
 * Provides a stepper UI for metadata, API headers, and endpoints configuration.
 */
@Component({
  standalone: true,
  selector: 'app-create-source-system',
  templateUrl: './create-source-system.component.html',
  styleUrls: ['./create-source-system.component.css'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DialogModule,
    DropdownModule,
    InputTextModule,
    ButtonModule,
    TextareaModule,
    ManageApiHeadersComponent,
    ManageEndpointsComponent,
    StepsModule,
    FileUploadModule,
  ]
})
export class CreateSourceSystemComponent implements OnInit, OnChanges {
  @Input() visible = false;
  @Input() sourceSystem: SourceSystemDTO | null = null;
  @Output() visibleChange = new EventEmitter<boolean>();



  steps = [
    {label: 'Metadaten'},
    {label: 'Api Header'},
    {label: 'Endpoints'},
  ];
  currentStep = 0;
  createdSourceSystemId!: number;

  form!: FormGroup;
  selectedFile: File | null = null;
  fileSelected = false;

  typeOptions = [
    {label: 'REST-OpenAPI', value: 'REST_OPENAPI'},
    {label: 'AAS', value: 'AAS'}
  ];
  authTypeOptions = [
    {label: 'Basic', value: ApiAuthType.Basic},
    {label: 'API Key', value: ApiAuthType.ApiKey}
  ];
  public readonly ApiAuthType = ApiAuthType;

  /**
   * @param fb FormBuilder for reactive form creation.
   * @param sourceSystemService Service to communicate with the SourceSystem backend API.
   */
  constructor(
    private fb: FormBuilder,
    private sourceSystemService: SourceSystemResourceService,
    protected errorService: HttpErrorService,
  ) {
  }

  /**
   * Initialize reactive form with metadata fields and validators.
   * Subscribes to authentication type changes to adjust validators dynamically.
   */
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
      openApiSpec: [{value: null, disabled: false}]
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

  /**
   * Reacts to incoming changes of the sourceSystem Input.
   * If editing an existing system, patches form values and resets to first step.
   *
   * @param changes Object containing changed @Input properties.
   */
  ngOnChanges(changes: SimpleChanges): void {
    if (changes['sourceSystem'] && this.sourceSystem) {
      this.form.patchValue({
        name: this.sourceSystem.name,
        apiUrl: this.sourceSystem.apiUrl,
        description: this.sourceSystem.description,
        apiType: this.sourceSystem.apiType
      });
      this.currentStep = 0;
    }
  }

  /**
   * Handles file selection for OpenAPI spec upload.
   * Disables the URL input when a file is chosen.
   *
   * @param ev File input change event.
   */
  onFileSelected(event: FileSelectEvent): void {
    this.selectedFile = event.files[0];
    this.fileSelected = true;
    this.form.get('openApiSpec')!.disable();
  }

  /**
   * Cancels creation/edit, resets form and component state, and closes the dialog.
   */
  cancel(): void {
    this.visible = false;
    this.visibleChange.emit(false);
    this.form.reset({apiType: 'REST_OPENAPI', apiAuthType: null});
    this.selectedFile = null;
    this.fileSelected = false;
    this.form.get('openApiSpec')!.enable();
    this.currentStep = 0;
  }

/**
 * Validates form, constructs DTO including authConfig and optional file blob or URL,
 * and triggers POST to create the Source System.
 */
save(): void {
  if (this.form.invalid) {
    this.form.markAllAsTouched();
    return;
  }

  const base = {...this.form.getRawValue()} as CreateSourceSystemDTO;
  delete base.authConfig;

  const authType = this.form.get('apiAuthType')!.value as ApiAuthType;
  const cfg = this.form.get('authConfig')!.value;
  if (authType === ApiAuthType.Basic) {
    base.authConfig = {authType, username: cfg.username, password: cfg.password} as BasicAuthDTO;
  } else if (authType === ApiAuthType.ApiKey) {
    base.authConfig = {authType, apiKey: cfg.apiKey, headerName: cfg.headerName} as ApiKeyAuthDTO;
  }

  const post = () => {
    this.postDto(base);
  };

  if (this.selectedFile) {
    delete base.openApiSpec;
    const reader = new FileReader();
    reader.onload = () => {
      const fileContent = reader.result as string;
      base.openApiSpec = fileContent;
      post();
    };
    reader.readAsText(this.selectedFile);

  } else {
    const openApiSpecValue = base.openApiSpec;
    if (openApiSpecValue && typeof openApiSpecValue === 'string' && openApiSpecValue.trim()) {
      base.openApiSpec = openApiSpecValue;
      post();
    } else {
      delete base.openApiSpec;
      post();
    }
  }
}

  /**
   * Performs HTTP POST to persist the Source System and handles Location header parsing.
   *
   * @param dto Prepared DTO for creation.
   */
  private postDto(dto: CreateSourceSystemDTO): void {
    console.log('📤 Sending DTO to backend:', dto);
    console.log('📤 openApiSpec field:', dto.openApiSpec);
    console.log('📤 openApiSpec type:', typeof dto.openApiSpec);

    this.sourceSystemService
      .apiConfigSourceSystemPost(dto)
      .subscribe({
        next: (resp: SourceSystemDTO) => {
          console.log('✅ Backend response:', resp);
          console.log('✅ Returned openApiSpec:', resp.openApiSpec);
          this.createdSourceSystemId = resp.id!;
          this.currentStep = 1;
        },
        error: (err) => {
          console.error('❌ CREATE failed:', err);
          this.errorService.handleError(err);
        }
      });
  }
  /**
   * Advances the stepper to the next step.
   * If on first step, saves the Source System before proceeding.
   */
  goNext(): void {
    if (this.currentStep === 0) {
      this.save();
    } else if (this.currentStep === 1) {
      this.currentStep = 2;
    }
  }


  /**
   * Moves the stepper to the previous step.
   */
  goBack(): void {
    if (this.currentStep > 0) {
      this.currentStep -= 1;
    }
  }

}
