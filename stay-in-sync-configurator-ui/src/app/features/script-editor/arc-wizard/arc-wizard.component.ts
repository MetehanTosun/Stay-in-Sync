import {
  Component,
  EventEmitter,
  inject,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
} from '@angular/core';
import {
  ApiRequestConfiguration,
  ArcSaveRequest,
  ArcTestCallRequest,
  ArcTestCallResponse,
  EndpointParameterDefinition,
} from '../models/arc.models';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ScriptEditorService } from '../../../core/services/script-editor.service';
import {
  SourceSystem,
  SourceSystemEndpoint,
} from '../../source-system/models/source-system.models';
import { DialogModule } from 'primeng/dialog';
import { CommonModule } from '@angular/common';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { DividerModule } from 'primeng/divider';
import { SchemaViewerComponent } from '../schema-viewer/schema-viewer.component';
import { catchError, finalize, of } from 'rxjs';
import { FieldsetModule } from 'primeng/fieldset';
import { DropdownModule } from 'primeng/dropdown';
import { TooltipModule } from 'primeng/tooltip';
import { MessagesModule } from 'primeng/messages';

import { HttpClient } from '@angular/common/http';
import { ArcStateService } from '../../../core/services/arc-state.service';

// TEMPORARY: FIX MESSAGING TOASTS AS A PATTERN
interface Message {
  severity?: 'success' | 'info' | 'warn' | 'error';
  summary?: string;
  detail?: string;
  life?: number;
}

@Component({
  selector: 'app-arc-wizard',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    CommonModule,
    DialogModule,
    InputTextModule,
    TooltipModule,
    FieldsetModule,
    DropdownModule,
    ButtonModule,
    DividerModule,
    SchemaViewerComponent,
    MessagesModule,
  ],
  templateUrl: './arc-wizard.component.html',
  styleUrl: './arc-wizard.component.css',
})
export class ArcWizardComponent implements OnChanges {
  @Input() visible = false;
  @Input() context!: {
    system: SourceSystem;
    endpoint: SourceSystemEndpoint;
    arcToClone?: ApiRequestConfiguration;
  };
  @Output() onHide = new EventEmitter<void>();
  @Output() onSaveSuccess = new EventEmitter<ApiRequestConfiguration>();

  arcForm!: FormGroup;
  isTesting = false;
  isSaving = false;
  isLoadingDefinitions = false;
  testResult: ArcTestCallResponse | null = null;
  errorMessages: Message[] = [];

  pathParams: EndpointParameterDefinition[] = [];
  queryParams: EndpointParameterDefinition[] = [];
  headers: EndpointParameterDefinition[] = [];

  private fb = inject(FormBuilder);
  private scriptEditorService = inject(ScriptEditorService);
  private http = inject(HttpClient);
  private arcStateService = inject(ArcStateService);

  get alias() {
    return this.arcForm.get('alias');
  }
  get pathParametersGroup() {
    return this.arcForm.get('pathParameters') as FormGroup;
  }
  get queryParametersGroup() {
    return this.arcForm.get('queryParameterValues') as FormGroup;
  }
  get headersGroup() {
    return this.arcForm.get('headerValues') as FormGroup;
  }

  constructor() {
    this.buildFormControls();
    this.arcForm = this.fb.group({
      alias: ['', Validators.required],
      pathParameters: this.fb.group({}),
      queryParameterValues: this.fb.group({}),
      headerValues: this.fb.group({}),
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['visible'] && this.visible && this.context) {
      this.resetWizardState();
      this.fetchAndBuildForm();
    }
  }

  private fetchAndBuildForm(): void {
    this.isLoadingDefinitions = true;
    this.errorMessages = [];

    this.scriptEditorService
      .getEndpointParameterDefinitions(this.context.endpoint.id)
      .pipe(
        catchError((err) => {
          this.errorMessages = [
            {
              severity: 'error',
              summary: 'Load Failed',
              detail: 'endpoint parameter definitions could not be loaded',
            },
          ];
          return of([]);
        })
      )
      .subscribe((definitions) => {
        this.pathParams = definitions.filter((d) => d.in === 'path');
        this.queryParams = definitions.filter((d) => d.in === 'query');
        this.headers = definitions.filter((d) => d.in === 'header');

        this.buildFormControls();
        this.isLoadingDefinitions = false;
      });
  }

  private buildFormControls(): void {
    const aliasControl = this.fb.control('', Validators.required);

    const buildGroup = (params: EndpointParameterDefinition[]): FormGroup => {
      const group: { [key: string]: FormControl } = {};
      params.forEach((p) => {
        const validators = p.required ? [Validators.required] : [];
        group[p.name] = this.fb.control('', validators);
      });
      return this.fb.group(group);
    };

    this.arcForm = this.fb.group({
      alias: aliasControl,
      pathParameters: buildGroup(this.pathParams),
      queryParameterValues: buildGroup(this.queryParams),
      headerValues: buildGroup(this.headers),
    });
    /**
    if (this.context.arcToClone) {
      // TODO: fetch the detailed ARC to get its saved parameter values for cloning
      this.arcForm.patchValue({
        alias: `${this.context.arcToClone?.alias}_copy`,
      });
    } */
  }

  onTestCall(): void {
    if (this.arcForm.invalid) {
      this.arcForm.markAllAsTouched();
      return;
    }
    this.isTesting = true;
    this.testResult = null;
    this.errorMessages = [];

    const formValue = this.arcForm.value;
    const request: ArcTestCallRequest = {
      sourceSystemId: this.context.system.id,
      endpointId: this.context.endpoint.id,
      pathParameters: this.cleanObject(formValue.pathParameters),
      queryParameterValues: this.cleanObject(formValue.queryParameterValues),
      headerValues: this.cleanObject(formValue.headerValues),
    };
    this.scriptEditorService
      .testArcConfiguration(request)
      .pipe(finalize(() => (this.isTesting = false)))
      .subscribe({
        next: (response) => {
          this.testResult = response;
          if (!response.isSuccess) {
            this.errorMessages = [
              {
                severity: 'warn',
                summary: 'Test Call Failed',
                detail: response.errorMessage,
              },
            ];
          }
        },
        error: (err) => {
          this.errorMessages = [
            {
              severity: 'error',
              summary: 'Test Call Error',
              detail: err.message || 'An unknown error occurred.',
            },
          ];
        },
      });
  }

  onSaveArc(): void {
    if (this.arcForm.invalid) {
      this.errorMessages = [
        {
          severity: 'warn',
          summary: 'Validation Error',
          detail: 'Please fill out all required fields.',
        },
      ];
      return;
    }
    if (!this.testResult?.isSuccess || !this.testResult.responsePayload) {
      this.errorMessages = [
        {
          severity: 'warn',
          summary: 'Test Required',
          detail:
            'Please run a successful "Test Call" before saving to generate the response schema.',
        },
      ];
      return;
    }

    this.isSaving = true;
    this.errorMessages = [];
    const formValue = this.arcForm.value;

    const createDto: ArcSaveRequest = {
      alias: formValue.alias,
      sourceSystemId: this.context.system.id,
      endpointId: this.context.endpoint.id,
      pathParameterValues: this.cleanObject(formValue.pathParameters),
      queryParameterValues: this.cleanObject(formValue.queryParameterValues),
      headerValues: this.cleanObject(formValue.headerValues),

      responseDts: this.testResult.responsePayload,

      pollingIntervallTimeInMs: 10000, // TODO: Implement in Form
    };

    this.http
      .post<ApiRequestConfiguration>(
        `/api/config/source-system/endpoint${this.context.endpoint.id}/request-configuration`,
        createDto
      )
      .pipe(finalize(() => (this.isSaving = false)))
      .subscribe({
        next: (savedArc) => {
          console.log('ARC saved successfully!', savedArc);

          this.arcStateService.addOrUpdateArc(savedArc);

          this.onSaveSuccess.emit(savedArc);
          this.closeDialog();
        },
        error: (err) => {
          this.errorMessages = [
            {
              severity: 'error',
              summary: 'Save Failed',
              detail: err.error?.message || 'Could not save the ARC.',
            },
          ];
        },
      });
  }

  closeDialog(): void {
    this.onHide.emit();
  }

  private resetWizardState(): void {
    this.testResult = null;
    this.errorMessages = [];
    this.arcForm.reset();
  }

  private cleanObject(obj: Record<string, any>): Record<string, string> {
    if (!obj) return {};
    const cleaned: Record<string, string> = {};
    for (const key in obj) {
      if (Object.prototype.hasOwnProperty.call(obj, key) && obj[key] !== null && obj[key] !== '') {
        cleaned[key] = String(obj[key]);
      }
    }
    return cleaned;
  }
}
