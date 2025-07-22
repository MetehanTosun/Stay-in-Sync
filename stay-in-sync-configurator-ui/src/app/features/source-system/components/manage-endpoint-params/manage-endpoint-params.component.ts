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
    // ggf. weitere Module
  ]
})
export class ManageEndpointParamsComponent implements OnInit, OnChanges {
  @Input() endpointId!: number;
  @Input() endpointPath?: string;

  queryParams: ApiEndpointQueryParamDTO[] = [];
  queryParamForm: FormGroup;
  queryParamsLoading = false;
  paramTypes = [
    { label: 'Query', value: ApiEndpointQueryParamType.Query },
    { label: 'Path', value: ApiEndpointQueryParamType.Path }
  ];

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

  ngOnInit(): void {
    if (this.endpointId) {
      this.loadQueryParams(this.endpointId);
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['endpointId'] && changes['endpointId'].currentValue) {
      this.loadQueryParams(changes['endpointId'].currentValue);
      this.queryParamForm.reset({ queryParamType: ApiEndpointQueryParamType.Query });
    }
  }

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

  deleteQueryParam(paramId: number) {
    this.queryParamSvc.apiConfigEndpointQueryParamIdDelete(paramId)
      .subscribe({
        next: () => {
          this.queryParams = this.queryParams.filter(p => p.id !== paramId);
        },
        error: (err) => console.error('Failed to delete query param', err)
      });
  }

  private ensureBraces(paramName: string): string {
    const cleanName = paramName.replace(/[{}]/g, '');
    return `{${cleanName}}`;
  }
} 