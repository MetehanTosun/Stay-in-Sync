// src/app/features/source-system/components/manage-api-headers/manage-api-headers.component.ts

import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { CardModule } from 'primeng/card';

import { ApiHeaderResourceService } from '../../generated/api/apiHeaderResource.service';
import { CreateApiHeaderDTO, ApiHeaderDTO, ApiRequestHeaderType } from '../../generated';

@Component({
  standalone: true,
  selector: 'app-manage-api-headers',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TableModule,
    ButtonModule,
    InputTextModule,
    DropdownModule,
    CardModule
  ],
  templateUrl: './manage-api-headers.component.html'
})
export class ManageApiHeadersComponent implements OnInit {
  @Input() syncSystemId!: number;              // hier übergibst du die ID (bspw. sourceSystemId)
  @Output() onCreated = new EventEmitter<void>();

  headers: ApiHeaderDTO[] = [];
  form!: FormGroup;
  loading = false;

  headerTypes = [
    { label: 'Accept',         value: ApiRequestHeaderType.Accept },
    { label: 'Content-Type',   value: ApiRequestHeaderType.ContentType },
    { label: 'Authorization',  value: ApiRequestHeaderType.Authorization },
    { label: 'User-Agent',     value: ApiRequestHeaderType.UserAgent },
    { label: 'Cache-Control',  value: ApiRequestHeaderType.CacheControl },
    { label: 'Accept-Charset', value: ApiRequestHeaderType.AcceptCharset },
    { label: 'Custom',         value: ApiRequestHeaderType.Custom }
  ];

  constructor(
    private fb: FormBuilder,
    private hdrSvc: ApiHeaderResourceService
  ) {}

  ngOnInit() {
    this.form = this.fb.group({
      headerName: ['', Validators.required],
      headerType: [ApiRequestHeaderType.Custom, Validators.required]
    });
    this.loadHeaders();
  }

  loadHeaders() {
    this.loading = true;
    this.hdrSvc
      .apiConfigSyncSystemSyncSystemIdRequestHeaderGet(this.syncSystemId)
      .subscribe({
        next: (list) => { this.headers = list; this.loading = false; },
        error: (err) => { console.error(err); this.loading = false; }
      });
  }

  addHeader() {
    if (this.form.invalid) return;
    const dto: CreateApiHeaderDTO = this.form.value;
    this.hdrSvc
      .apiConfigSyncSystemSyncSystemIdRequestHeaderPost(this.syncSystemId, dto)
      .subscribe({
        next: () => {
          this.form.reset({ headerType: ApiRequestHeaderType.Custom });
          this.loadHeaders();
          this.onCreated.emit();
        },
        error: console.error
      });
  }

  deleteHeader(id: number) {
    this.hdrSvc
      .apiConfigSyncSystemRequestHeaderIdDelete(id)
      .subscribe({
        next: () => this.headers = this.headers.filter(h => h.id !== id),
        error: console.error
      });
  }
}