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
import { finalize } from 'rxjs';
import { FieldsetModule } from 'primeng/fieldset';
import { Dropdown, DropdownModule } from 'primeng/dropdown';
import { TooltipModule } from 'primeng/tooltip';
import { MessagesModule } from 'primeng/messages';

import { HttpClient } from '@angular/common/http';
import { ArcStateService } from '../../../core/services/arc-state.service';
import { InputNumberModule } from 'primeng/inputnumber';
import { TableModule } from 'primeng/table';
import { InputSwitchModule } from 'primeng/inputswitch';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TabViewModule } from 'primeng/tabview';
import { AccordionModule } from 'primeng/accordion';

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
    AccordionModule,
    TableModule,
    ProgressSpinnerModule,
    TabViewModule,
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
  private arcStateService = inject(ArcStateService);

  /** @description Getter for the 'alias' form control for easy template access. */
  get alias() {
    return this.arcForm.get('alias');
  }
  /** @description Getter for the 'pathParameters' form group for easy template access. */
  get pathParametersGroup() {
    return this.arcForm.get('pathParameters') as FormGroup;
  }
  /** @description Getter for the 'queryParameters' form array for easy template access. */
  get queryParameters() {
    return this.arcForm.get('queryParameters') as FormArray;
  }
  /** @description Getter for the 'headerParameters' form array for easy template access. */
  get headerParameters() {
    return this.arcForm.get('headerParameters') as FormArray;
  }

  constructor() {
    const minimumPollingRate = 1;
    const defaultPollingRate = 1000;

    this.arcForm = this.fb.group({
      alias: ['', Validators.required],
      pollingRate: [
        defaultPollingRate,
        [Validators.required, Validators.min(minimumPollingRate)],
      ],
      pathParameters: this.fb.group({}),
      queryParameters: this.fb.array([]),
      headerParameters: this.fb.array([]),
    });
  }

  /**
   * @description Angular lifecycle hook that responds to changes in data-bound input properties.
   * Triggers the initialization of the wizard when it becomes visible.
   * @param {SimpleChanges} changes - An object of key-value pairs mapping property names to SimpleChange objects.
   */
  ngOnChanges(changes: SimpleChanges): void {
    const visibilityConfig = 'visible';
    if (changes[visibilityConfig] && this.visible && this.context) {
      this.resetWizardState();
      this.loadInitialData();
    }
  }

  /**
   * @private
   * @description Fetches the necessary context data (parameter definitions, header definitions) for the wizard.
   * It then initializes the form controls and populates them if in 'edit' or 'clone' mode.
   */
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
        const pathParameterControlName = 'pathParameters';
        this.arcForm.setControl(
          pathParameterControlName,
          this.fb.group(pathParamControls)
        );

        this.queryParameters.clear();
        this.queryParamDefinitions = data.queryParamDefinitions;

        const requiredQueryParams = this.queryParamDefinitions.filter(
          (p) => p.required
        );
        this.availableQueryParams = this.queryParamDefinitions.filter(
          (p) => !p.required
        );

        requiredQueryParams.forEach((p) => {
          const keyName = p.name;
          const paramValue = '';
          const isDefined = true;
          const isRequired = true;
          const parameterType = p.type!;

          this.queryParameters.push(
            this.newParam(
              keyName,
              paramValue,
              isDefined,
              isRequired,
              parameterType
            )
          );
        });

        this.headerParameters.clear();
        this.headerDefinitions = data.headerDefinitions;

        this.availableHeaders = [...data.headerDefinitions];

        const arcToLoad = this.context.arcToEdit || this.context.arcToClone;
        if (arcToLoad) {
          this.scriptEditorService
            .getArcDetails(arcToLoad.id)
            .subscribe((arcDetails) => {
              this.populateForm(arcDetails);
              this.isLoadingDefinitions = false;
            });
        } else {
          this.isLoadingDefinitions = false;
        }
      });
  }

  /**
   * @private
   * @description Fills the wizard's form with data from an existing ARC configuration.
   * Handles both editing an existing ARC and cloning one to create a new ARC.
   * @param {ApiRequestConfiguration} arc - The ARC configuration to load into the form.
   */
  private populateForm(arc: ApiRequestConfiguration): void {
    const isClone = !!this.context.arcToClone;
    const alias = isClone ? `${arc.alias}_copy` : arc.alias;
    const defaultPollingRate1s = 1000;
    this.arcForm.patchValue({
      alias: alias,
      pollingRate:
        (arc as any).pollingIntervallTimeInMs || defaultPollingRate1s,
    });

    this.queryParameters.clear();
    this.headerParameters.clear();

    const queryParamsFromDto = (arc as any).apiRequestParameters || [];
    queryParamsFromDto.forEach(
      (param: { type: string; paramName: string; paramValue: string }) => {
        const queryParamType = 'QUERY';
        if (param.type === queryParamType) {
          const predefined = this.queryParamDefinitions.find(
            (p) => p.name === param.paramName
          );
          this.queryParameters.push(
            this.newParam(
              param.paramName,
              param.paramValue,
              !!predefined,
              predefined?.required || false,
              predefined?.type || 'string'
            )
          );
          this.availableQueryParams = this.availableQueryParams.filter(
            (p) => p.name !== param.paramName
          );
        }
      }
    );

    const headersFromDto = (arc as any).apiRequestHeaders || [];
    headersFromDto.forEach(
      (header: { headerName: string; values: string[] }) => {
        const headerValue =
          header.values && header.values.length > 0 ? header.values[0] : '';
        const predefined = this.headerDefinitions.find(
          (h) => h.headerName === header.headerName
        );
        this.headerParameters.push(
          this.newParam(
            header.headerName,
            headerValue,
            !!predefined,
            false,
            'string'
          )
        );
        this.availableHeaders = this.availableHeaders.filter(
          (h) => h.headerName !== header.headerName
        );
      }
    );
  }

  /**
   * @private
   * @description A factory method to create a new FormGroup for a parameter (query or header).
   * @param {string} key - The name of the parameter.
   * @param {string} value - The initial value of the parameter.
   * @param {boolean} isDefined - Whether the parameter is predefined by the system's endpoint.
   * @param {boolean} isRequired - Whether the parameter is required.
   * @param {string} type - The data type of the parameter (e.g., 'string', 'number').
   * @returns {FormGroup} A new FormGroup representing the parameter.
   */
  private newParam(
    key: string,
    value: string,
    isDefined: boolean,
    isRequired: boolean,
    type: string
  ): FormGroup {
    const valueValidators = isRequired ? [Validators.required] : [];
    return this.fb.group({
      key: [{ value: key, disabled: isDefined }, Validators.required],
      value: [value, valueValidators],
      isDefined: [isDefined],
      isRequired: [isRequired],
      type: [type],
    });
  }

  /**
   * @description Adds a predefined query parameter to the form from the dropdown list.
   * @param {EndpointParameterDefinition | null} param - The parameter definition to add.
   */
  addPredefinedQueryParam(param: EndpointParameterDefinition | null): void {
    if (!param) return;

    this.queryParameters.push(
      this.newParam(param.name, '', true, param.required, param.type!)
    );
    this.availableQueryParams = this.availableQueryParams.filter(
      (p) => p.name !== param.name
    );

    setTimeout(() => {
      this.paramDropdown?.clear();
    }, 0);
  }

  /**
   * @description Adds a predefined header to the form from the dropdown list.
   * @param {ApiHeaderDefinition | null} header - The header definition to add.
   */
  addPredefinedHeader(header: ApiHeaderDefinition | null): void {
    if (!header) return;

    const keyName = header.headerName;
    const paramValue = '';
    const isDefined = true;
    const isRequired = false;
    const parameterType = 'string';

    this.headerParameters.push(
      this.newParam(keyName, paramValue, isDefined, isRequired, parameterType)
    );
    this.availableHeaders = this.availableHeaders.filter(
      (h) => h.headerName !== header.headerName
    );

    setTimeout(() => {
      this.headerDropdown?.clear();
    }, 0);
  }

  /**
   * @description Adds a new query parameter to the form, either custom or predefined.
   * @param {EndpointParameterDefinition} [paramToAdd] - The optional predefined parameter to add. If not provided, a blank custom parameter is added.
   */
  addQueryParam(paramToAdd?: EndpointParameterDefinition): void {
    if (paramToAdd) {
      const keyName = paramToAdd.name;
      const paramValue = '';
      const isDefined = true;
      const isRequired = paramToAdd.required;
      const parameterType = paramToAdd.type!;

      this.queryParameters.push(
        this.newParam(keyName, paramValue, isDefined, isRequired, parameterType)
      );
      this.availableQueryParams = this.availableQueryParams.filter(
        (p) => p.name !== paramToAdd.name
      );
    } else {
      const keyName = '';
      const paramValue = '';
      const isDefined = false;
      const isRequired = false;
      const parameterType = 'string';

      this.queryParameters.push(
        this.newParam(keyName, paramValue, isDefined, isRequired, parameterType)
      );
    }
  }

  /**
   * @description Removes a query parameter from the form at a specific index.
   * If the parameter was predefined, it is returned to the list of available parameters.
   * @param {number} index - The index of the query parameter to remove.
   */
  removeQueryParam(index: number): void {
    const removedControl = this.queryParameters.at(index);
    const paramName = removedControl.get('key')?.value;

    if (removedControl.get('isDefined')?.value) {
      const predefinedParam = this.queryParamDefinitions.find(
        (p) => p.name === paramName
      );
      if (predefinedParam) {
        this.availableQueryParams.push(predefinedParam);
        this.availableQueryParams.sort((a, b) => a.name.localeCompare(b.name));
      }
    }

    this.queryParameters.removeAt(index);
  }

  /**
   * @description Adds a new header to the form, either custom or predefined.
   * @param {ApiHeaderDefinition} [headerToAdd] - The optional predefined header to add. If not provided, a blank custom header is added.
   */
  addHeader(headerToAdd?: ApiHeaderDefinition): void {
    if (headerToAdd) {
      const keyName = headerToAdd.headerName;
      const paramValue = '';
      const isDefined = true;
      const isRequired = false;
      const parameterType = 'string';

      this.headerParameters.push(
        this.newParam(keyName, paramValue, isDefined, isRequired, parameterType)
      );
      this.availableHeaders = this.availableHeaders.filter(
        (h) => h.headerName !== headerToAdd.headerName
      );
    } else {
      this.headerParameters.push(this.newParam('', '', false, false, 'string'));
    }
  }

  /**
   * @description Removes a header from the form at a specific index.
   * If the header was predefined, it is returned to the list of available headers.
   * @param {number} index - The index of the header to remove.
   */
  removeHeader(index: number): void {
    const removedControl = this.headerParameters.at(index);
    const headerName = removedControl.get('key')?.value;

    if (removedControl.get('isDefined')?.value) {
      const predefinedHeader = this.headerDefinitions.find(
        (h) => h.headerName === headerName
      );
      if (predefinedHeader) {
        this.availableHeaders.push(predefinedHeader);
        this.availableHeaders.sort((a, b) =>
          a.headerName.localeCompare(b.headerName)
        );
      }
    }

    this.headerParameters.removeAt(index);
  }

  /**
   * @description Executes a live test call to the backend with the current form configuration.
   * Displays the response payload and schema, or an error message if the call fails.
   */
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

  /**
   * @private
   * @description Recursively traverses a payload object to create a "pruned" version.
   * For arrays, it only keeps the first element to represent the structure, reducing the size
   * of the generated type definition.
   * @param {any} data - The data payload to prune.
   * @returns {any} The pruned data structure.
   */
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

  /**
   * @description Handles the save action for the ARC. Validates the form, ensures a successful
   * test call has been made, constructs the save DTO, and sends it to the backend.
   */
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

    const prunedPayload = this.prunePayloadRecursively(
      this.testResult.responsePayload
    );

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

    this.scriptEditorService
      .saveArcConfiguration(saveRequest)
      .pipe(finalize(() => (this.isSaving = false)))
      .subscribe({
        next: (savedArc) => {
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

  /**
   * @description Emits the onHide event to close the dialog.
   */
  closeDialog(): void {
    this.onHide.emit();
  }

  /**
   * @private
   * @description Resets the entire state of the wizard to its initial, clean state.
   * This includes clearing test results, error messages, and the form itself.
   */
  private resetWizardState(): void {
    this.testResult = null;
    this.errorMessages = [];
    this.arcForm.reset();
    this.queryParameters.clear();
    this.headerParameters.clear();
  }

  /**
   * @private
   * @description A utility function to clean an object by removing null or empty string properties.
   * @param {Record<string, any>} obj - The object to clean.
   * @returns {Record<string, string>} The cleaned object with all values converted to strings.
   */
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

  /**
   * @description A utility function to convert a FormArray of key-value pairs into a Record/Map.
   * @param {FormArray} formArray - The FormArray to convert.
   * @returns {Record<string, string>} The resulting key-value map.
   */
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
