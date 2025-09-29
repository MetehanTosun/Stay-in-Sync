import {
  Component,
  EventEmitter,
  inject,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  ViewChild,
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
import {ScriptEditorService} from '../../../core/services/script-editor.service';
import {
  SourceSystem,
  SourceSystemEndpoint,
} from '../../source-system/models/source-system.models';
import {DialogModule} from 'primeng/dialog';
import {CommonModule} from '@angular/common';
import {InputTextModule} from 'primeng/inputtext';
import {ButtonModule} from 'primeng/button';
import {DividerModule} from 'primeng/divider';
import {SchemaViewerComponent} from '../schema-viewer/schema-viewer.component';
import {catchError, finalize, of} from 'rxjs';
import {FieldsetModule} from 'primeng/fieldset';
import {Dropdown, DropdownModule} from 'primeng/dropdown';
import {TooltipModule} from 'primeng/tooltip';
import {MessagesModule} from 'primeng/messages';

import {HttpClient} from '@angular/common/http';
import {ArcStateService} from '../../../core/services/arc-state.service';
import {InputNumberModule} from 'primeng/inputnumber';
import {TableModule} from 'primeng/table';
import {InputSwitchModule} from 'primeng/inputswitch';

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
    TableModule,
    SchemaViewerComponent,
    InputNumberModule,
    InputSwitchModule,
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
    arcToEdit?: ApiRequestConfiguration;
    arcToClone?: ApiRequestConfiguration;
  };
  @Output() onHide = new EventEmitter<void>();
  @Output() onSaveSuccess = new EventEmitter<ApiRequestConfiguration>();

  @ViewChild('paramDropdown') paramDropdown: Dropdown | undefined;
  @ViewChild('headerDropdown') headerDropdown: Dropdown | undefined;

  arcForm!: FormGroup;
  isTesting = false;
  isSaving = false;
  isLoadingDefinitions = false;
  testResult: ArcTestCallResponse | null = null;
  errorMessages: Message[] = [];

  pathParamDefinitions: EndpointParameterDefinition[] = [];
  queryParamDefinitions: EndpointParameterDefinition[] = [];
  headerDefinitions: ApiHeaderDefinition[] = [];

  availableQueryParams: EndpointParameterDefinition[] = [];
  availableHeaders: ApiHeaderDefinition[] = [];

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
      pollingRate: [1000, [Validators.required, Validators.min(1)]],
      pathParameters: this.fb.group({}),
      queryParameters: this.fb.array([]),
      headerParameters: this.fb.array([]),
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['visible'] && this.visible && this.context) {
      this.resetWizardState();
      this.loadInitialData();
    }
  }

  private loadInitialData(): void {
    this.isLoadingDefinitions = true;
    this.errorMessages = [];

    this.scriptEditorService
      .getArcWizardContextData(this.context.system.id, this.context.endpoint.id)
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
        this.queryParamDefinitions = data.queryParamDefinitions;

        const requiredQueryParams = this.queryParamDefinitions.filter(p => p.required);
        this.availableQueryParams = this.queryParamDefinitions.filter(p => !p.required);

        requiredQueryParams.forEach(p => {
          this.queryParameters.push(this.newParam(p.name, '', true, true, p.type!));
        });

        this.headerParameters.clear();
        this.headerDefinitions = data.headerDefinitions;

        this.availableHeaders = [...data.headerDefinitions];

        const arcToLoad = this.context.arcToEdit || this.context.arcToClone;
        if (arcToLoad) {
          this.scriptEditorService.getArcDetails(arcToLoad.id).subscribe(arcDetails => {
            this.populateForm(arcDetails);
            this.isLoadingDefinitions = false;
          });
        } else {
          this.isLoadingDefinitions = false;
        }
      });
  }

  private populateForm(arc: ApiRequestConfiguration): void {
    const isClone = !!this.context.arcToClone;
    const alias = isClone ? `${arc.alias}_copy` : arc.alias;

    this.arcForm.patchValue({
      alias: alias,
      pollingRate: (arc as any).pollingIntervallTimeInMs || 1000,
    });

    this.queryParameters.clear();
    this.headerParameters.clear();

    const queryParamsFromDto = (arc as any).apiRequestParameters || [];
    queryParamsFromDto.forEach((param: { type: string; paramName: string; paramValue: string }) => {
      if (param.type === 'QUERY') {
        const predefined = this.queryParamDefinitions.find(p => p.name === param.paramName);
        this.queryParameters.push(
          this.newParam(
            param.paramName,
            param.paramValue,
            !!predefined,
            predefined?.required || false,
            predefined?.type || 'string'
          )
        );
        this.availableQueryParams = this.availableQueryParams.filter(p => p.name !== param.paramName);
      }
    });

    const headersFromDto = (arc as any).apiRequestHeaders || [];
    headersFromDto.forEach((header: { headerName: string; values: string[] }) => {
      const headerValue = header.values && header.values.length > 0 ? header.values[0] : '';
      const predefined = this.headerDefinitions.find(h => h.headerName === header.headerName);
      this.headerParameters.push(
        this.newParam(
          header.headerName,
          headerValue,
          !!predefined,
          false,
          'string'
        )
      );
      this.availableHeaders = this.availableHeaders.filter(h => h.headerName !== header.headerName);
    });
  }

  private newParam(
    key: string,
    value: string,
    isDefined: boolean,
    isRequired: boolean,
    type: string
  ): FormGroup {
    const valueValidators = isRequired ? [Validators.required] : [];
    return this.fb.group({
      key: [{value: key, disabled: isDefined}, Validators.required],
      value: [value, valueValidators],
      isDefined: [isDefined],
      isRequired: [isRequired],
      type: [type],
    });
  }

  addPredefinedQueryParam(param: EndpointParameterDefinition | null): void {
    if (!param) return;

    this.queryParameters.push(this.newParam(param.name, '', true, param.required, param.type!));
    this.availableQueryParams = this.availableQueryParams.filter(p => p.name !== param.name);

    setTimeout(() => {
      this.paramDropdown?.clear();
    }, 0);
  }

  addPredefinedHeader(header: ApiHeaderDefinition | null): void {
    if (!header) return;

    this.headerParameters.push(this.newParam(header.headerName, '', true, false, 'string'));
    this.availableHeaders = this.availableHeaders.filter(h => h.headerName !== header.headerName);

    setTimeout(() => {
      this.headerDropdown?.clear();
    }, 0);
  }

  addQueryParam(paramToAdd?: EndpointParameterDefinition): void {
    if (paramToAdd) {
      this.queryParameters.push(this.newParam(paramToAdd.name, '', true, paramToAdd.required, paramToAdd.type!));
      this.availableQueryParams = this.availableQueryParams.filter(p => p.name !== paramToAdd.name);
    } else {
      this.queryParameters.push(this.newParam('', '', false, false, 'string'));
    }
  }

  removeQueryParam(index: number): void {
    const removedControl = this.queryParameters.at(index);
    const paramName = removedControl.get('key')?.value;

    if (removedControl.get('isDefined')?.value) {
      const predefinedParam = this.queryParamDefinitions.find(p => p.name === paramName);
      if (predefinedParam) {
        this.availableQueryParams.push(predefinedParam);
        this.availableQueryParams.sort((a, b) => a.name.localeCompare(b.name));
      }
    }

    this.queryParameters.removeAt(index);
  }

  addHeader(headerToAdd?: ApiHeaderDefinition): void {
    if (headerToAdd) {
      this.headerParameters.push(this.newParam(headerToAdd.headerName, '', true, false, 'string'));
      this.availableHeaders = this.availableHeaders.filter(h => h.headerName !== headerToAdd.headerName);
    } else {
      this.headerParameters.push(this.newParam('', '', false, false, 'string'));
    }
  }

  removeHeader(index: number): void {
    const removedControl = this.headerParameters.at(index);
    const headerName = removedControl.get('key')?.value;

    if (removedControl.get('isDefined')?.value) {
      const predefinedHeader = this.headerDefinitions.find(h => h.headerName === headerName);
      if (predefinedHeader) {
        this.availableHeaders.push(predefinedHeader);
        this.availableHeaders.sort((a, b) => a.headerName.localeCompare(b.headerName));
      }
    }

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

  private prunePayloadRecursively(data: any): any {
    if (data == null || typeof data !== 'object') {
      return data;
    }

    if (Array.isArray(data)) {
      if (data.length === 0) {
        return [];
      }
      return [this.prunePayloadRecursively(data[0])];
    }

    const newObject: { [key: string]: any } = {};
    for (const key in data) {
      if (Object.prototype.hasOwnProperty.call(data, key)) {
        newObject[key] = this.prunePayloadRecursively(data[key]);
      }
    }
    return newObject;
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

    const prunedPayload = this.prunePayloadRecursively(this.testResult.responsePayload);

    const saveRequest: ArcSaveRequest = {
      id: this.context.arcToEdit ? this.context.arcToEdit.id : undefined,
      alias: this.alias?.value,
      sourceSystemId: this.context.system.id,
      endpointId: this.context.endpoint.id,
      pathParameterValues: this.cleanObject(this.pathParametersGroup.value),
      queryParameterValues: queryParameterValues,
      headerValues: headerValues,
      responseDts: JSON.stringify(prunedPayload),
      pollingIntervallTimeInMs: this.arcForm.get('pollingRate')?.value,
    };

    this.scriptEditorService.saveArcConfiguration(saveRequest)
      .pipe(finalize(() => (this.isSaving = false)))
      .subscribe({
        next: (savedArc) => {
          console.log('ARC saved/updated successfully!', savedArc);

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
