import { Component, OnInit, Input } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule as PrimeInputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { CardModule } from 'primeng/card';
import { StepsModule } from 'primeng/steps';
import { MenuItem } from 'primeng/api';
import { SourceSystemEndpointResourceService } from '../../../../generated/api/sourceSystemEndpointResource.service';
import { SourceSystemEndpointDTO } from '../../../../generated/model/sourceSystemEndpointDTO';
import { CreateSourceSystemEndpointDTO } from '../../../../generated/model/createSourceSystemEndpointDTO';

@Component({
  standalone: true,
  selector: 'app-manage-endpoints',
  templateUrl: './manage-endpoints.component.html',
  styleUrls: ['./manage-endpoints.component.css'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TableModule,
    ButtonModule,
    PrimeInputTextModule,
    DropdownModule,
    CardModule,
    StepsModule,
  ]
})
export class ManageEndpointsComponent implements OnInit {
  @Input() sourceSystemId!: number;

  endpoints: SourceSystemEndpointDTO[] = [];
  endpointForm!: FormGroup;
  loading = false;

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
    { label: 'DELETE', value: 'DELETE' }
  ];

  constructor(
    private fb: FormBuilder,
    private endpointService: SourceSystemEndpointResourceService
  ) {}

  ngOnInit(): void {
    this.endpointForm = this.fb.group({
      endpointPath: ['', Validators.required],
      httpRequestType: ['GET', Validators.required]
    });
    this.loadEndpoints();
  }

  loadEndpoints(): void {
    if (!this.sourceSystemId) return;
    this.loading = true;
    this.endpointService
      .apiConfigSourceSystemSourceSystemIdEndpointGet(this.sourceSystemId)
      .subscribe({
        next: endpoints => {
          this.endpoints = endpoints;
          this.loading = false;
        },
        error: err => {
          console.error('Failed to load endpoints:', err);
          this.loading = false;
        }
      });
  }

  addEndpoint(): void {
    if (this.endpointForm.invalid) return;
    const newE = this.endpointForm.value as CreateSourceSystemEndpointDTO;
    this.endpointService
      .apiConfigSourceSystemSourceSystemIdEndpointPost(this.sourceSystemId, [newE])
      .subscribe({
        next: () => {
          this.endpointForm.reset({ httpRequestType: 'GET' });
          this.loadEndpoints();
        },
        error: err => console.error(err)
      });
  }

  deleteEndpoint(endpointId: number): void {
    this.endpointService.apiConfigSourceSystemEndpointIdDelete(endpointId).subscribe({
      next: () => this.endpoints = this.endpoints.filter(e => e.id !== endpointId),
      error: err => console.error('Failed to delete endpoint:', err)
    });
  }
}