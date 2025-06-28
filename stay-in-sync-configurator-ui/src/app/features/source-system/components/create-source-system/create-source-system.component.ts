import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { FormBuilder, FormGroup, Validators, FormsModule } from '@angular/forms';
import { SourceSystemApiService } from '../../../../services/source-system-api.service';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { tap } from 'rxjs/operators';

// PrimeNG modules
import { DialogModule } from 'primeng/dialog';
import { StepsModule } from 'primeng/steps';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextarea } from 'primeng/inputtextarea';
import { InputGroupModule } from 'primeng/inputgroup';
import { TableModule } from 'primeng/table';

interface Step { label: string; }

@Component({
  selector: 'app-create-source-system',
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
    InputGroupModule,
    TableModule,
    FormsModule
  ],
  templateUrl: './create-source-system.component.html',
  styleUrls: ['./create-source-system.component.css']
})
export class CreateSourceSystemComponent implements OnInit {
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();

  form!: FormGroup;
  currentStep = 1;
  steps: Step[] = [
    { label: 'Metadata' },
    { label: 'Endpoints' },
    { label: 'Specification' }
  ];

  typeOptions = [
    { label: 'AAS', value: 'AAS' },
    { label: 'REST-OpenAPI', value: 'REST_OPENAPI' }
  ];
  authTypeOptions = [
    { label: 'Basic', value: 'BASIC' },
    { label: 'API Key', value: 'API_KEY' }
  ];

  // State
  createdSourceId: number | null = null;
  endpoints: any[] = [];
  newEndpointName = '';

  // File upload
  selectedFile: File | null = null;
  fileSelected = false;

  constructor(
    private fb: FormBuilder,
    public api: SourceSystemApiService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      name: ['', Validators.required],
      description: [''],
      type: ['', Validators.required],
      apiUrl: ['', Validators.required],
      authType: [''],
      username: [''],
      password: [''],
      apiKey: [''],
      openApiSpec: ['']
    });
  }

  open(): void {
    this.visible = true;
    this.visibleChange.emit(true);
  }

  cancel(): void {
    this.visible = false;
    this.visibleChange.emit(false);
    this.resetAll();
  }

  private resetAll() {
    this.currentStep = 1;
    this.createdSourceId = null;
    this.endpoints = [];
    this.form.reset();
    this.selectedFile = null;
    this.fileSelected = false;
    this.newEndpointName = '';
  }

  prev(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  next(): void {
    if (this.currentStep === 1) {
      // Create source system
      const dto: any = {
        name: this.form.value.name,
        description: this.form.value.description,
        type: this.form.value.type,
        apiUrl: this.form.value.apiUrl,
        authType: this.form.value.authType,
        username: this.form.value.username,
        password: this.form.value.password,
        apiKey: this.form.value.apiKey
      };
      this.api.create(dto).pipe(
        tap((created: any) => {
          this.createdSourceId = created.id;
          this.loadEndpoints();
          this.currentStep = 2;
        })
      ).subscribe();
      return;
    }

    if (this.currentStep === 2) {
      // when on Endpoints step, move to Spec
      this.currentStep = 3;
      return;
    }

    // Step 3: handle specification submission/upload
    const dto: any = {
      // same metadata + maybe spec URL
    };
    // handle file vs URL, then finish
    this.finish();
  }

  onFileSelected(event: Event): void {
    const inp = event.target as HTMLInputElement;
    if (inp.files && inp.files.length) {
      this.selectedFile = inp.files[0];
      this.fileSelected = true;
      this.form.patchValue({ openApiSpec: '' });
    }
  }

  private finish(): void {
    this.cancel();
  }

  // --- Endpoints handling ---

  loadEndpoints(): void {
    if (!this.createdSourceId) return;
    this.api.listEndpoints(this.createdSourceId).subscribe(e => this.endpoints = e);
  }

  addEndpoint(): void {
    if (!this.createdSourceId || !this.newEndpointName) return;
    const payload = { name: this.newEndpointName /* plus other fields */ };
    this.api.createEndpoint(this.createdSourceId, payload).subscribe(() => {
      this.newEndpointName = '';
      this.loadEndpoints();
    });
  }

  /**
   * Extracts JSON schema for the given endpoint and reloads the list.
   */
  public onExtract(endp: any): void {
    if (!this.createdSourceId) {
      return;
    }
    this.api.extractEndpointSchema(this.createdSourceId, endp.id)
      .subscribe(() => this.loadEndpoints());
  }
}