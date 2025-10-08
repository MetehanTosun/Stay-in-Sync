import { Component, Input, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { CardModule } from 'primeng/card';
import { ApiEndpointQueryParamResourceService } from '../../service/apiEndpointQueryParamResource.service';
import { ApiEndpointQueryParamDTO } from '../../models/apiEndpointQueryParamDTO';
import { ApiEndpointQueryParamType } from '../../models/apiEndpointQueryParamType';
import {FloatLabel} from 'primeng/floatlabel';
import {Select} from 'primeng/select';

/**
 * Component to manage API endpoint query parameters.
 * Allows viewing, adding, and deleting query parameters for a given endpoint.
 */
@Component({
  standalone: true,
  selector: 'app-manage-endpoint-params',
  templateUrl: './manage-endpoint-params.component.html',
  styleUrls: ['./manage-endpoint-params.component.css'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TableModule,
    ButtonModule,
    InputTextModule,
    DropdownModule,
    CardModule,
    FloatLabel,
    Select,
    // ggf. weitere Module
  ]
})
export class ManageEndpointParamsComponent implements OnInit, OnChanges {
  /**
   * The ID of the API endpoint whose parameters are managed.
   */
  @Input() endpointId!: number;

  /**
   * The path of the API endpoint (optional).
   */
  @Input() endpointPath?: string;

  /**
   * List of query parameters for the current endpoint.
   */
  queryParams: ApiEndpointQueryParamDTO[] = [];

  /**
   * Form group for adding new query parameters.
   */
  queryParamForm: FormGroup;

  /**
   * Indicates whether the query parameters are currently being loaded.
   */
  queryParamsLoading = false;

  /**
   * Available parameter types for selection in the form.
   */
  paramTypes = [
    { label: 'Query', value: ApiEndpointQueryParamType.Query },
    { label: 'Path', value: ApiEndpointQueryParamType.Path }
  ];

  /**
   * Controls whether the component is expanded or collapsed.
   */
  isExpanded = true;

  /**
   * Toggles the expanded/collapsed state of the component.
   */
  toggleExpanded() {
    this.isExpanded = !this.isExpanded;
  }

  /**
   * Constructor injecting FormBuilder and ApiEndpointQueryParamResourceService.
   * @param fb FormBuilder for creating reactive forms.
   * @param queryParamSvc Service to interact with API endpoint query parameters.
   */
  constructor(
    private fb: FormBuilder,
    private queryParamSvc: ApiEndpointQueryParamResourceService
  ) {
    // FormGroup immer initialisieren
    this.queryParamForm = this.fb.group({
      paramName: ['', Validators.required],
      queryParamType: [ApiEndpointQueryParamType.Query, Validators.required]
    });
  }

  /**
   * Angular lifecycle hook called once the component is initialized.
   * Loads query parameters if an endpoint ID is provided.
   */
  ngOnInit(): void {
    if (this.endpointId) {
      this.loadQueryParams(this.endpointId);
    }
  }

  /**
   * Angular lifecycle hook called when input properties change.
   * Reloads query parameters when the endpoint ID changes.
   * @param changes Object containing the changed input properties.
   */
  ngOnChanges(changes: SimpleChanges): void {
    if (changes['endpointId'] && changes['endpointId'].currentValue) {
      this.loadQueryParams(changes['endpointId'].currentValue);
      this.queryParamForm.reset({ queryParamType: ApiEndpointQueryParamType.Query });
    }
  }

  /**
   * Loads the query parameters for the specified endpoint ID.
   * Updates the queryParams array and loading state.
   * @param endpointId The ID of the endpoint to load parameters for.
   */
  loadQueryParams(endpointId: number) {
    this.queryParamsLoading = true;
    this.queryParamSvc.apiConfigEndpointEndpointIdQueryParamGet(endpointId)
      .subscribe({
        next: (params) => {
          this.queryParams = params;
          this.queryParamsLoading = false;
        },
        error: (err) => {
          console.error('Failed to load query params', err);
          this.queryParamsLoading = false;
        }
      });
  }

  /**
   * Adds a new query parameter using the form data.
   * Resets the form and reloads the query parameters upon success.
   */
  addQueryParam() {
    if (this.queryParamForm.invalid || !this.endpointId) {
      return;
    }
    let dto: ApiEndpointQueryParamDTO = { ...this.queryParamForm.value };
    if (dto.queryParamType === ApiEndpointQueryParamType.Path) {
      dto.paramName = this.ensureBraces(dto.paramName!);
    }
    this.queryParamSvc.apiConfigEndpointEndpointIdQueryParamPost(this.endpointId, dto)
      .subscribe({
        next: () => {
          this.queryParamForm.reset({ queryParamType: ApiEndpointQueryParamType.Query });
          this.loadQueryParams(this.endpointId);
        },
        error: (err) => console.error('Failed to add query param', err)
      });
  }

  /**
   * Deletes a query parameter by its ID.
   * Removes the parameter from the local list upon success.
   * @param paramId The ID of the query parameter to delete.
   */
  deleteQueryParam(paramId: number) {
    this.queryParamSvc.apiConfigEndpointQueryParamIdDelete(paramId)
      .subscribe({
        next: () => {
          this.queryParams = this.queryParams.filter(p => p.id !== paramId);
        },
        error: (err) => console.error('Failed to delete query param', err)
      });
  }

  /**
   * Ensures that a path parameter name is enclosed in curly braces.
   * Removes existing braces before adding new ones.
   * @param paramName The parameter name to format.
   * @returns The parameter name enclosed in curly braces.
   */
  private ensureBraces(paramName: string): string {
    const cleanName = paramName.replace(/[{}]/g, '');
    return `{${cleanName}}`;
  }
}
