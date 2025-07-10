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
  ApiHeaderDefinition,
  ApiRequestConfiguration,
  ArcSaveRequest,
  ArcTestCallRequest,
  ArcTestCallResponse,
  EndpointParameterDefinition,
} from '../models/arc.models';
import {
  FormArray,
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

  pathParamDefinitions: EndpointParameterDefinition[] = [];
  queryParamDefinitions: EndpointParameterDefinition[] = [];
  headerDefinitions: ApiHeaderDefinition[] = [];

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
  get queryParameters() {
    return this.arcForm.get('queryParameters') as FormArray;
  }
  get headerParameters() {
    return this.arcForm.get('headerParameters') as FormArray;
  }

  constructor() {
    this.arcForm = this.fb.group({
      alias: ['', Validators.required],
      pathParameters: this.fb.group({}),
      queryParameters: this.fb.array([]),
      headerParameters: this.fb.array([]),
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
      .getArcWizardContextData(this.context.system.id, this.context.endpoint.id)
      .pipe(
        catchError((err) => {
          this.errorMessages = [
            {
              severity: 'error',
              summary: 'Load Failed',
              detail: 'Could not load parameter and header definitions.',
            },
          ];
          return of(null);
        })
      )
      .subscribe((data) => {
        if (!data) {
          this.isLoadingDefinitions = false;
          return;
        }

        this.pathParamDefinitions = data.pathParams;
        this.queryParamDefinitions = data.queryParamDefinitions;
        this.headerDefinitions = data.headerDefinitions;

        const pathParamControls: { [key: string]: FormControl } = {};
        data.pathParams.forEach((p) => {
          const validators = p.required ? [Validators.required] : [];
          pathParamControls[p.name] = this.fb.control('', validators);
        });
        this.arcForm.setControl(
          'pathParameters',
          this.fb.group(pathParamControls)
        );

        this.queryParameters.clear();
        data.queryParamDefinitions.forEach((p) => {
          this.queryParameters.push(
            this.newParam(p.name, '', true, p.required)
          );
        });

        this.headerParameters.clear();
        data.headerDefinitions.forEach((h) => {
          this.headerParameters.push(
            this.newParam(h.headerName, '', true, false)
          );
        });

        this.isLoadingDefinitions = false;
      });
  }

  private newParam(
    key: string,
    value: string,
    isDefined: boolean,
    isRequired: boolean
  ): FormGroup {
    const valueValidators = isRequired ? [Validators.required] : [];
    return this.fb.group({
      key: [{ value: key, disabled: isDefined }, Validators.required],
      value: [value, valueValidators],
      isDefined: [isDefined],
      isRequired: [isRequired],
    });
  }

  addCustomQueryParam(): void {
    this.queryParameters.push(this.newParam('', '', false, false));
  }

  removeQueryParam(index: number): void {
    this.queryParameters.removeAt(index);
  }

  addCustomHeader(): void {
    this.headerParameters.push(this.newParam('', '', false, false));
  }

  removeHeader(index: number): void {
    this.headerParameters.removeAt(index);
  }

  /**
    if (this.context.arcToClone) {
      // TODO: fetch the detailed ARC to get its saved parameter values for cloning
      this.arcForm.patchValue({
        alias: `${this.context.arcToClone?.alias}_copy`,
      });
    } */

  onTestCall(): void {
    if (this.arcForm.invalid) {
      this.arcForm.markAllAsTouched();
      return;
    }
    this.isTesting = true;
    this.testResult = null;
    this.errorMessages = [];

    const queryParameterValues = this.formArrayToMap(this.queryParameters);
    const headerValues = this.formArrayToMap(this.headerParameters);

    const request: ArcTestCallRequest = {
      sourceSystemId: this.context.system.id,
      endpointId: this.context.endpoint.id,
      pathParameters: this.cleanObject(this.pathParametersGroup.value),
      queryParameterValues: queryParameterValues,
      headerValues: headerValues,
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
              detail: err.message || 'An unknown error occured.',
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
      this.arcForm.markAllAsTouched();
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
    const queryParameterValues = this.formArrayToMap(this.queryParameters);
    const headerValues = this.formArrayToMap(this.headerParameters);

    const createDto: ArcSaveRequest = {
      alias: this.alias?.value,
      sourceSystemId: this.context.system.id,
      endpointId: this.context.endpoint.id,
      pathParameterValues: this.cleanObject(this.pathParametersGroup.value),
      queryParameterValues: queryParameterValues,
      headerValues: headerValues,
      responseDts: JSON.stringify(this.testResult.responsePayload),
      pollingIntervallTimeInMs: 10000, // TODO: Implement in Form
    };

    this.http
      .post<ApiRequestConfiguration>(
        `/api/config/source-system/endpoint/${this.context.endpoint.id}/request-configuration`, // TODO verifiy endpoint
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
    this.queryParameters.clear();
    this.headerParameters.clear();
  }

  private cleanObject(obj: Record<string, any>): Record<string, string> {
    if (!obj) return {};
    const cleaned: Record<string, string> = {};
    for (const key in obj) {
      if (
        Object.prototype.hasOwnProperty.call(obj, key) &&
        obj[key] !== null &&
        obj[key] !== ''
      ) {
        cleaned[key] = String(obj[key]);
      }
    }
    return cleaned;
  }

  formArrayToMap(formArray: FormArray): Record<string, string> {
    const map: Record<string, string> = {};

    formArray.getRawValue().forEach((param: { key: string; value: string }) => {
      if (param.key && param.value !== null && param.value !== '') {
        map[param.key] = String(param.value);
      }
    });

    return map;
  }
}
