import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { CardModule } from 'primeng/card';
import { StepsModule } from 'primeng/steps';
import { CheckboxModule } from 'primeng/checkbox';
import { MenuItem } from 'primeng/api';

import { SourceSystemEndpointResourceService } from '../../../../generated/api/sourceSystemEndpointResource.service';
import { RequestConfigurationResourceService } from '../../../../generated/api/requestConfigurationResource.service';
import { SourceSystemEndpointDTO } from '../../../../generated/model/sourceSystemEndpointDTO';
import { CreateSourceSystemEndpointDTO } from '../../../../generated/model/createSourceSystemEndpointDTO';
import { CreateRequestConfigurationDTO } from '../../../../generated/model/createRequestConfigurationDTO';
import { GetRequestConfigurationDTO } from '../../../../generated/model/getRequestConfigurationDTO';

@Component({
  standalone: true,
  selector: 'app-manage-endpoints',
  templateUrl: './manage-endpoints.component.html',
  styleUrls: ['./manage-endpoints.component.css'],
  imports: [
    CommonModule,
    TableModule,
    ButtonModule,
    InputTextModule,
    DropdownModule,
    CardModule,
    StepsModule,
    CheckboxModule,
    ReactiveFormsModule,
  ]
})
export class ManageEndpointsComponent implements OnInit {
  @Input() sourceSystemId!: number;
  @Output() backStep = new EventEmitter<void>();
  @Output() finish = new EventEmitter<void>();

  // endpoints
  endpoints: SourceSystemEndpointDTO[] = [];
  endpointForm!: FormGroup;
  loading = false;

  // selected endpoint
  selectedEndpoint: SourceSystemEndpointDTO | null = null;

  // request-configs
  requestConfigurations: GetRequestConfigurationDTO[] = [];
  requestConfigurationForm!: FormGroup;
  loadingConfigs = false;

  // stepper
  steps: MenuItem[] = [
    { label: 'Metadata' },
    { label: 'Endpoints' },
    { label: 'Headers' },
    { label: 'Query Params' },
    { label: 'Request Config' },
    { label: 'Specification' },
  ];
  activeIndex = 1;

  httpRequestTypes = [
    { label: 'GET', value: 'GET' },
    { label: 'POST', value: 'POST' },
    { label: 'PUT', value: 'PUT' },
    { label: 'DELETE', value: 'DELETE' },
  ];

  constructor(
    private fb: FormBuilder,
    private endpointSvc: SourceSystemEndpointResourceService,
    private configSvc: RequestConfigurationResourceService
  ) {}

  ngOnInit(): void {
    // endpoint form
    this.endpointForm = this.fb.group({
      endpointPath: ['', Validators.required],
      httpRequestType: ['GET', Validators.required],
    });
    // config form
    this.requestConfigurationForm = this.fb.group({
      name: ['', Validators.required],
      pollingIntervallTimeInMs: [1000, [Validators.required, Validators.min(1)]],
      used: [false],
    });
    this.loadEndpoints();
  }

  // --- endpoints CRUD ---
  loadEndpoints() {
    if (!this.sourceSystemId) return;
    this.loading = true;
    this.endpointSvc
      .apiConfigSourceSystemSourceSystemIdEndpointGet(this.sourceSystemId)
      .subscribe({
        next: eps => {
          this.endpoints = eps;
          this.loading = false;
        },
        error: err => {
          console.error(err);
          this.loading = false;
        },
      });
  }

  addEndpoint() {
    if (this.endpointForm.invalid) return;
    const dto = this.endpointForm.value as CreateSourceSystemEndpointDTO;
    this.endpointSvc
      .apiConfigSourceSystemSourceSystemIdEndpointPost(this.sourceSystemId, [dto])
      .subscribe({
        next: () => {
          this.endpointForm.reset({ httpRequestType: 'GET' });
          this.loadEndpoints();
        },
        error: console.error,
      });
  }

  deleteEndpoint(id: number) {
    this.endpointSvc
      .apiConfigSourceSystemEndpointIdDelete(id)
      .subscribe({
        next: () => this.endpoints = this.endpoints.filter(e => e.id !== id),
        error: console.error,
      });
  }

  // when user clicks “Manage” on a row:
  manage(endpoint: SourceSystemEndpointDTO) {
    this.selectedEndpoint = endpoint;
    console.log('Managing endpoint', endpoint);
    this.activeIndex = 4;           // go to request-config step
    this.loadRequestConfigs();
  }

  // --- request‐configs CRUD ---
  loadRequestConfigs() {
    this.loadingConfigs = true;
    this.configSvc
      .apiConfigSourceSystemSourceSystemIdRequestConfigurationGet(this.sourceSystemId)
      .subscribe({
        next: (configs: GetRequestConfigurationDTO[]) => {
          this.requestConfigurations = configs;
          this.loadingConfigs = false;
        },
        error: err => {
          console.error(err);
          this.loadingConfigs = false;
        },
      });
  }

  addRequestConfig() {
    if (!this.selectedEndpoint || this.requestConfigurationForm.invalid) return;
    const dto = this.requestConfigurationForm.value as CreateRequestConfigurationDTO;
    this.configSvc
      .apiConfigSourceSystemEndpointEndpointIdRequestConfigurationPost(this.selectedEndpoint.id!, dto)
      .subscribe({
        next: () => {
          this.requestConfigurationForm.reset({ used: false, pollingIntervallTimeInMs: 1000 });
          this.loadRequestConfigs();
        },
        error: console.error,
      });
  }

  deleteRequestConfig(id: number) {
    this.configSvc
      .apiConfigSourceSystemEndpointRequestConfigurationIdDelete(id)
      .subscribe({
        next: () => this.requestConfigurations = this.requestConfigurations.filter(c => c.id !== id),
        error: console.error,
      });
  }

  onBack() { this.backStep.emit(); }
  onFinish() { this.finish.emit(); }
}